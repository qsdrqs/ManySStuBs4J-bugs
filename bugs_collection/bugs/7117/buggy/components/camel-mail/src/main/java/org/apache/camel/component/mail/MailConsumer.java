/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.mail;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderNotFoundException;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledBatchPollingConsumer;
import org.apache.camel.support.SynchronizationAdapter;
import org.apache.camel.util.CastUtils;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.apache.camel.Consumer Consumer} which consumes messages from JavaMail using a
 * {@link javax.mail.Transport Transport} and dispatches them to the {@link Processor}
 */
public class MailConsumer extends ScheduledBatchPollingConsumer {
    public static final String POP3_UID = "CamelPop3Uid";
    public static final long DEFAULT_CONSUMER_DELAY = 60 * 1000L;
    private static final Logger LOG = LoggerFactory.getLogger(MailConsumer.class);

    private final JavaMailSender sender;
    private Folder folder;
    private Store store;

    public MailConsumer(MailEndpoint endpoint, Processor processor, JavaMailSender sender) {
        super(endpoint, processor);
        this.sender = sender;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (folder != null && folder.isOpen()) {
            folder.close(true);
        }
        if (store != null && store.isConnected()) {
            store.close();
        }

        super.doStop();
    }

    protected int poll() throws Exception {
        // must reset for each poll
        shutdownRunningTask = null;
        pendingExchanges = 0;
        int polledMessages = 0;

        ensureIsConnected();

        if (store == null || folder == null) {
            throw new IllegalStateException("MailConsumer did not connect properly to the MailStore: "
                    + getEndpoint().getConfiguration().getMailStoreLogInformation());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Polling mailbox folder: " + getEndpoint().getConfiguration().getMailStoreLogInformation());
        }

        if (getEndpoint().getConfiguration().getFetchSize() == 0) {
            LOG.warn("Fetch size is 0 meaning the configuration is set to poll no new messages at all. Camel will skip this poll.");
            return 0;
        }

        // ensure folder is open
        if (!folder.isOpen()) {
            folder.open(Folder.READ_WRITE);
        }

        try {
            int count = folder.getMessageCount();
            if (count > 0) {
                Message[] messages;

                if (getEndpoint().getSearchTerm() != null) {
                    // use custom search term
                    messages = folder.search(getEndpoint().getSearchTerm());
                } else if (getEndpoint().getConfiguration().isUnseen()) {
                    // only unseen messages
                    messages = folder.search(new SearchTermBuilder().unseen().build());
                } else {
                    // get all messages
                    messages = folder.getMessages();
                }

                polledMessages = processBatch(CastUtils.cast(createExchanges(messages)));
            } else if (count == -1) {
                throw new MessagingException("Folder: " + folder.getFullName() + " is closed");
            }
        } catch (Exception e) {
            handleException(e);
        } finally {
            // need to ensure we release resources, but only if closeFolder or disconnect = true
            if (getEndpoint().getConfiguration().isCloseFolder() || getEndpoint().getConfiguration().isDisconnect()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Close mailbox folder {} from {}", folder.getName(), getEndpoint().getConfiguration().getMailStoreLogInformation());
                }
                try {
                    if (folder.isOpen()) {
                        folder.close(true);
                    }
                } catch (Exception e) {
                    // some mail servers will lock the folder so we ignore in this case (CAMEL-1263)
                    LOG.debug("Could not close mailbox folder: " + folder.getName() + ". This exception is ignored.", e);
                }
            }
        }

        // should we disconnect, the header can override the configuration
        boolean disconnect = getEndpoint().getConfiguration().isDisconnect();
        if (disconnect) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Disconnecting from {}", getEndpoint().getConfiguration().getMailStoreLogInformation());
            }
            try {
                store.close();
            } catch (Exception e) {
                LOG.debug("Could not disconnect from {}: " + getEndpoint().getConfiguration().getMailStoreLogInformation() + ". This exception is ignored.", e);
            }
            store = null;
            folder = null;
        }

        return polledMessages;
    }

    public int processBatch(Queue<Object> exchanges) throws Exception {
        int total = exchanges.size();

        // limit if needed
        if (maxMessagesPerPoll > 0 && total > maxMessagesPerPoll) {
            LOG.debug("Limiting to maximum messages to poll {} as there was {} messages in this poll.", maxMessagesPerPoll, total);
            total = maxMessagesPerPoll;
        }

        for (int index = 0; index < total && isBatchAllowed(); index++) {
            // only loop if we are started (allowed to run)
            Exchange exchange = ObjectHelper.cast(Exchange.class, exchanges.poll());
            // add current index and total as properties
            exchange.setProperty(Exchange.BATCH_INDEX, index);
            exchange.setProperty(Exchange.BATCH_SIZE, total);
            exchange.setProperty(Exchange.BATCH_COMPLETE, index == total - 1);

            // update pending number of exchanges
            pendingExchanges = total - index - 1;

            // must use the original message in case we need to workaround a charset issue when extracting mail content
            final Message mail = exchange.getIn(MailMessage.class).getOriginalMessage();

            // need to call setPeek on java-mail to avoid the message being flagged eagerly as SEEN on the server in case
            // we process the message and rollback due an exception
            if (getEndpoint().getConfiguration().isPeek()) {
                peekMessage(mail);
            }

            // add on completion to handle after work when the exchange is done
            exchange.addOnCompletion(new SynchronizationAdapter() {
                public void onComplete(Exchange exchange) {
                    processCommit(mail, exchange);
                }

                public void onFailure(Exchange exchange) {
                    processRollback(mail, exchange);
                }

                @Override
                public boolean allowHandover() {
                    // do not allow handover as the commit/rollback logic needs to be executed
                    // on the same session that polled the messages
                    return false;
                }

                @Override
                public String toString() {
                    return "MailConsumerOnCompletion";
                }
            });

            // process the exchange
            processExchange(exchange);
        }

        return total;
    }

    private void peekMessage(Message mail) {
        // this only applies to IMAP messages which has a setPeek method
        if (mail.getClass().getName().startsWith("IMAP")) {
            try {
                LOG.trace("Calling setPeek(true) on mail message {}", mail);
                IntrospectionSupport.setProperty(mail, "peek", true);
            } catch (Throwable e) {
                // ignore
                LOG.trace("Error setting peak property to true on: " + mail + ". This exception is ignored.", e);
            }
        }
    }

    protected Queue<Exchange> createExchanges(Message[] messages) throws MessagingException {
        Queue<Exchange> answer = new LinkedList<Exchange>();

        int fetchSize = getEndpoint().getConfiguration().getFetchSize();
        int count = fetchSize == -1 ? messages.length : Math.min(fetchSize, messages.length);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetching {} messages. Total {} messages.", count, messages.length);
        }

        for (int i = 0; i < count; i++) {
            Message message = messages[i];

            if (LOG.isTraceEnabled()) {
                LOG.trace("Mail #{} is of type: {} - {}", new Object[]{i, ObjectHelper.classCanonicalName(message), message});
            }

            if (!message.getFlags().contains(Flags.Flag.DELETED)) {
                Exchange exchange = getEndpoint().createExchange(message);
                if (getEndpoint().getConfiguration().isMapMailMessage()) {
                    // ensure the mail message is mapped, which can be ensured by touching the body/header/attachment
                    LOG.trace("Mapping #{} from javax.mail.Message to Camel MailMessage", i);
                    exchange.getIn().getBody();
                    exchange.getIn().getHeaders();
                    exchange.getIn().getAttachments();
                }

                // If the protocol is POP3 we need to remember the uid on the exchange
                // so we can find the mail message again later to be able to delete it
                if (getEndpoint().getConfiguration().getProtocol().startsWith("pop3")) {
                    String uid = generatePop3Uid(message);
                    if (uid != null) {
                        exchange.setProperty(POP3_UID, uid);
                        LOG.trace("POP3 mail message using uid {}", uid);
                    }
                }
                answer.add(exchange);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping message as it was flagged as deleted: {}", MailUtils.dumpMessage(message));
                }
            }
        }

        return answer;
    }

    /**
     * Strategy to process the mail message.
     */
    protected void processExchange(Exchange exchange) throws Exception {
        if (LOG.isDebugEnabled()) {
            MailMessage msg = (MailMessage) exchange.getIn();
            LOG.debug("Processing message: {}", MailUtils.dumpMessage(msg.getMessage()));
        }
        getProcessor().process(exchange);
    }

    /**
     * Strategy to flag the message after being processed.
     *
     * @param mail     the mail message
     * @param exchange the exchange
     */
    protected void processCommit(Message mail, Exchange exchange) {
        try {
            // ensure folder is open
            if (!folder.isOpen()) {
                folder.open(Folder.READ_WRITE);
            }

            // If the protocol is POP3, the message needs to be synced with the folder via the UID.
            // Otherwise setting the DELETE/SEEN flag won't delete the message.
            String uid = (String) exchange.removeProperty(POP3_UID);
            if (uid != null) {
                int count = folder.getMessageCount();
                Message found = null;
                LOG.trace("Looking for POP3Message with UID {} from folder with {} mails", uid, count);
                for (int i = 1; i <= count; ++i) {
                    Message msg = folder.getMessage(i);
                    if (uid.equals(generatePop3Uid(msg))) {
                        LOG.debug("Found POP3Message with UID {} from folder with {} mails", uid, count);
                        found = msg;
                        break;
                    }
                }

                if (found == null) {
                    boolean delete = getEndpoint().getConfiguration().isDelete();
                    LOG.warn("POP3message not found in folder. Message cannot be marked as " + (delete ? "DELETED" : "SEEN"));
                } else {
                    mail = found;
                }
            }

            org.apache.camel.Message in = exchange.getIn();
            MailConfiguration config = getEndpoint().getConfiguration();
            // header values override configuration values
            String copyTo = in.getHeader("copyTo", config.getCopyTo(), String.class);
            boolean delete = in.getHeader("delete", config.isDelete(), boolean.class);

            // Copy message into different imap folder if asked
            if (config.getProtocol().equals(MailUtils.PROTOCOL_IMAP) || config.getProtocol().equals(MailUtils.PROTOCOL_IMAPS)) {
                if (copyTo != null) {
                    LOG.trace("IMAP message needs to be copied to {}", copyTo);
                    Folder destFolder = store.getFolder(copyTo);
                    if (!destFolder.exists()) {
                        destFolder.create(Folder.HOLDS_MESSAGES);
                    }
                    folder.copyMessages(new Message[]{mail}, destFolder);
                    LOG.trace("IMAP message {} copied to {}", mail, copyTo);
                }
            }

            if (delete) {
                LOG.trace("Exchange processed, so flagging message as DELETED");
                mail.setFlag(Flags.Flag.DELETED, true);
            } else {
                LOG.trace("Exchange processed, so flagging message as SEEN");
                mail.setFlag(Flags.Flag.SEEN, true);
            }
        } catch (MessagingException e) {
            getExceptionHandler().handleException("Error occurred during committing mail message: " + mail, exchange, e);
        }
    }

    /**
     * Strategy when processing the exchange failed.
     *
     * @param mail     the mail message
     * @param exchange the exchange
     */
    protected void processRollback(Message mail, Exchange exchange) {
        Exception cause = exchange.getException();
        if (cause != null) {
            LOG.warn("Exchange failed, so rolling back message status: " + exchange, cause);
        } else {
            LOG.warn("Exchange failed, so rolling back message status: " + exchange);
        }
    }

    /**
     * Generates an UID of the POP3Message
     *
     * @param message the POP3Message
     * @return the generated uid
     */
    protected String generatePop3Uid(Message message) {
        String uid = null;

        // create an UID based on message headers on the POP3Message, that ought
        // to be unique
        StringBuilder buffer = new StringBuilder();
        try {
            Enumeration<?> it = message.getAllHeaders();
            while (it.hasMoreElements()) {
                Header header = (Header)it.nextElement();
                buffer.append(header.getName()).append("=").append(header.getValue()).append("\n");
            }
            if (buffer.length() > 0) {
                LOG.trace("Generating UID from the following:\n {}", buffer);
                uid = UUID.nameUUIDFromBytes(buffer.toString().getBytes()).toString();
            }
        } catch (MessagingException e) {
            LOG.warn("Cannot reader headers from mail message. This exception will be ignored.", e);
        }

        return uid;
    }

    private void ensureIsConnected() throws MessagingException {
        MailConfiguration config = getEndpoint().getConfiguration();

        boolean connected = false;
        try {
            if (store != null && store.isConnected()) {
                connected = true;
            }
        } catch (Exception e) {
            LOG.debug("Exception while testing for is connected to MailStore: "
                    + getEndpoint().getConfiguration().getMailStoreLogInformation()
                    + ". Caused by: " + e.getMessage(), e);
        }

        if (!connected) {
            // ensure resources get recreated on reconnection
            store = null;
            folder = null;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Connecting to MailStore: {}", getEndpoint().getConfiguration().getMailStoreLogInformation());
            }
            store = sender.getSession().getStore(config.getProtocol());
            store.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
        }

        if (folder == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Getting folder {}", config.getFolderName());
            }
            folder = store.getFolder(config.getFolderName());
            if (folder == null || !folder.exists()) {
                throw new FolderNotFoundException(folder, "Folder not found or invalid: " + config.getFolderName());
            }
        }
    }

    @Override
    public MailEndpoint getEndpoint() {
        return (MailEndpoint) super.getEndpoint();
    }

}