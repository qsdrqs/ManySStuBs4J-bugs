package org.openhab.binding.maxcul.internal.messages;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.openhab.io.transport.cul.CULCommunicationException;
import org.openhab.io.transport.cul.CULHandler;
import org.openhab.io.transport.cul.CULListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle messages going to and from the CUL device. Make sure to intercept control command
 * responses first before passing on valid MAX! messages to the binding itself for processing.
 *
 * @author Paul Hampson (cyclingengineer)
 * @since 1.5.0
 */
public class MaxCulMsgHandler implements CULListener {

	private static final Logger logger =
			LoggerFactory.getLogger(MaxCulMsgHandler.class);

	class SenderQueueItem {
		BaseMsg msg;
		Date expiry;
		int retryCount = 0;
	}

	private int surplusCredit = 0;
	private Date lastTransmit;
	private Date endOfQueueTransmit;

	private int msgCount = 0;
	private CULHandler cul = null;
	private String srcAddr;
	private HashMap<Integer, BaseMsg> callbackRegister;
	private LinkedList<SenderQueueItem> sendQueue;
	private HashMap<Byte, SenderQueueItem> pendingAckQueue;
	private MaxCulBindingMessageProcessor mcbmp = null;
	private Map<SenderQueueItem, Timer> timers = new HashMap<SenderQueueItem,Timer>();

	private final int MESSAGE_EXPIRY_PERIOD = 30000;

	public MaxCulMsgHandler(String srcAddr, CULHandler cul)
	{
		this.cul = cul;
		cul.registerListener(this);
		this.srcAddr = srcAddr;
		this.callbackRegister = new HashMap<Integer, BaseMsg>();
		this.sendQueue = new LinkedList<SenderQueueItem>();
		this.pendingAckQueue = new HashMap<Byte, SenderQueueItem>();
		this.lastTransmit = new Date(); /* init as now */
		this.endOfQueueTransmit = this.lastTransmit;
	}

	private byte getMessageCount()
	{
		this.msgCount += 1;
		this.msgCount &= 0xFF;
		return (byte)this.msgCount;
	}

	private boolean enoughCredit(int requiredCredit)
	{
		return enoughCredit(requiredCredit, false);
	}

	private boolean enoughCredit(int requiredCredit, boolean updateSurplus)
	{
		Date now = new Date();
		/* units are accumulated as 1% of time elapsed with no TX */
		long credit = ((now.getTime() - this.lastTransmit.getTime())/100)+this.surplusCredit;

		/* assume we need preamble which is 100 credits (1000ms) */
		// TODO handle 'fast sending' if device is awake
		boolean result = (credit > (requiredCredit+100));
		if (result && updateSurplus)
		{
			this.surplusCredit = (int)credit - (requiredCredit+100);
			/* accumulate a max of 1hr credit */
			if (this.surplusCredit > 360) this.surplusCredit = 360;
		}

		return result;
	}

	private void transmitMessage( BaseMsg data )
	{
		try {
			cul.send(data.rawMsg);
		} catch (CULCommunicationException e) {
			logger.error("Unable to send CUL message "+data+" because: "+e.getMessage());
		}
		/* update surplus credit value */
		enoughCredit(data.requiredCredit(),true);
		this.lastTransmit = new Date();
		if (this.endOfQueueTransmit.before(this.lastTransmit))
		{
			/* hit a time after the queue finished tx'ing */
			this.endOfQueueTransmit = this.lastTransmit;
		}

		/* awaiting ack now */
		// TODO different behaviour if callback
		SenderQueueItem qi = new SenderQueueItem();
		qi.msg = data;
		qi.expiry = new Date(this.lastTransmit.getTime()+MESSAGE_EXPIRY_PERIOD);
		this.pendingAckQueue.put(qi.msg.msgCount, qi);
	}

	private void sendMessage( BaseMsg msg )
	{
		Timer timer = null;

		if (msg.readyToSend())
		{
			if (enoughCredit(msg.requiredCredit()) && this.sendQueue.isEmpty())
			{
				/* send message as we have enough credit and nothing is on the queue waiting */
				logger.debug("Sending message immediately. Message is "+msg.msgType+" => "+msg.rawMsg);
        		transmitMessage(msg);
			} else {
				/* messages ahead of us so queue up the item and schedule a task to process it */
				SenderQueueItem qi = new SenderQueueItem();
				qi.msg = msg;
				TimerTask task = new TimerTask() {
	                public void run() {
	                	SenderQueueItem topItem = sendQueue.remove();
	                	logger.debug("Checking credit");
	                	if (enoughCredit(topItem.msg.requiredCredit()))
	                	{
	                		logger.debug("Sending item from queue. Message is "+topItem.msg.msgType+" => "+topItem.msg.rawMsg);
	                		transmitMessage(topItem.msg);
	                	} else {
	                		logger.error("Not enough credit after waiting. This is bad. Queued command is discarded");
	                	}
	                }
				};

				timer = new Timer();
				timers.put(qi, timer);
				/* calculate when we want to TX this item in the queue, with a margin of 2 credits*/
				this.endOfQueueTransmit = new Date(this.endOfQueueTransmit.getTime() + ((msg.requiredCredit()+2)*10));
				timer.schedule(task, this.endOfQueueTransmit);
				this.sendQueue.add(qi);

				logger.debug("Added message to queue to be TX'd at "+this.endOfQueueTransmit.toString());
			}
		} else logger.debug("Tried to send a message that wasn't ready!");
	}

	/**
	 * Associate binding processor with this message handler
	 * @param mcbmp Binding processor to associate with this message handler
	 */
	public void registerMaxCulBindingMessageProcessor( MaxCulBindingMessageProcessor mcbmp )
	{
		if (this.mcbmp == null)
		{
			this.mcbmp = mcbmp;
			logger.debug("Associated MaxCulBindingMessageProcessor");
		}
		else
			logger.error("Tried to associate a second MaxCulBindingMessageProcessor!");
	}

	public void checkPendingAcks()
	{
		Date now = new Date();

		for (SenderQueueItem qi : pendingAckQueue.values())
		{
			if (now.after(qi.expiry))
			{
				logger.error("Packet lost - timeout");
				pendingAckQueue.remove(qi.msg.msgCount);
			}
		}
	}

	@Override
	public void dataReceived(String data) {
		logger.debug("MaxCulSender Received "+data);
		if (data.startsWith("Z"))
		{
			/* Handle ACKs */
			MaxCulMsgType msgType = BaseMsg.getMsgType(data);
			if (msgType == MaxCulMsgType.ACK)
			{
				AckMsg msg = new AckMsg(data);
				if (pendingAckQueue.containsKey(msg.msgCount))
				{
					SenderQueueItem qi = pendingAckQueue.remove(msg.msgCount);
					/* verify ack */
					if ((qi.msg.dstAddrStr.compareToIgnoreCase(msg.srcAddrStr) == 0) &&
							(qi.msg.srcAddrStr.compareToIgnoreCase(msg.dstAddrStr) == 0))
							{
								if (msg.getIsNack())
								{
									/* NAK'd! */
									// TODO resend?
									logger.error("Message was NAK'd, packet lost");
								} else logger.debug("Message "+msg.msgCount+" ACK'd ok!");

							}
				} else logger.info("Got ACK for message "+msg.msgCount+" but it wasn't in the queue");
			}
			/* TODO look for any messages that have a matching entry in the callback register */

			/* pass data to binding for processing */
			this.mcbmp.MaxCulMsgReceived(data);
		}
	}

	@Override
	public void error(Exception e) {
		/* Ignore errors for now - not sure what I would need to handle here at the moment
		 * TODO lookup error cases
		 */
	}

	/**
	 * Send response to PairPing
	 * @param dstAddr Address of device to respond to
	 */
	public void sendPairPong(String dstAddr)
	{
		PairPongMsg pp = new PairPongMsg(getMessageCount(), (byte)0, MaxCulMsgType.PAIR_PONG, (byte) 0, this.srcAddr, dstAddr);
		sendMessage(pp);
	}



}
