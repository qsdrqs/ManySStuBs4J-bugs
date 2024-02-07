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
package org.apache.camel.builder.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.camel.Exchange;
import org.apache.camel.ExpectedBodyTypeException;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeTransformException;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.converter.jaxp.XmlErrorListener;
import org.apache.camel.support.SynchronizationAdapter;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.util.ObjectHelper.notNull;

/**
 * Creates a <a href="http://camel.apache.org/processor.html">Processor</a>
 * which performs an XSLT transformation of the IN message body.
 * <p/>
 * Will by default output the result as a String. You can chose which kind of output
 * you want using the <tt>outputXXX</tt> methods.
 *
 * @version 
 */
public class XsltBuilder implements Processor {
    private final static Logger LOG = LoggerFactory.getLogger(XsltBuilder.class);
    private Map<String, Object> parameters = new HashMap<String, Object>();
    private XmlConverter converter = new XmlConverter();
    private Templates template;
    private ResultHandlerFactory resultHandlerFactory = new StringResultHandlerFactory();
    private boolean failOnNullBody = true;
    private URIResolver uriResolver;
    private boolean deleteOutputFile;
    private ErrorListener errorListener = new XsltErrorListener();
    private boolean allowStAX;

    public XsltBuilder() {
    }

    public XsltBuilder(Templates templates) {
        this.template = templates;
    }

    @Override
    public String toString() {
        return "XSLT[" + template + "]";
    }

    public void process(Exchange exchange) throws Exception {
        notNull(getTemplate(), "template");

        if (isDeleteOutputFile()) {
            // add on completion so we can delete the file when the Exchange is done
            String fileName = ExchangeHelper.getMandatoryHeader(exchange, Exchange.XSLT_FILE_NAME, String.class);
            exchange.addOnCompletion(new XsltBuilderOnCompletion(fileName));
        }

        Transformer transformer = getTemplate().newTransformer();
        configureTransformer(transformer, exchange);
        transformer.setErrorListener(new DefaultTransformErrorHandler());
        ResultHandler resultHandler = resultHandlerFactory.createResult(exchange);
        Result result = resultHandler.getResult();

        // lets copy the headers before we invoke the transform in case they modify them
        Message out = exchange.getOut();
        out.copyFrom(exchange.getIn());

        // the underlying input stream, which we need to close to avoid locking files or other resources
        InputStream is = null;
        try {
            Source source;
            // only convert to input stream if really needed
            if (isInputStreamNeeded(exchange)) {
                is = exchange.getIn().getBody(InputStream.class);
                source = getSource(exchange, is);
            } else {
                Object body = exchange.getIn().getBody();
                source = getSource(exchange, body);
            }
            LOG.trace("Using {} as source", source);
            transformer.transform(source, result);
            LOG.trace("Transform complete with result {}", result);
            resultHandler.setBody(out);
        } finally {
            // IOHelper can handle if is is null
            IOHelper.close(is);
        }
    }

    // Builder methods
    // -------------------------------------------------------------------------

    /**
     * Creates an XSLT processor using the given templates instance
     */
    public static XsltBuilder xslt(Templates templates) {
        return new XsltBuilder(templates);
    }

    /**
     * Creates an XSLT processor using the given XSLT source
     */
    public static XsltBuilder xslt(Source xslt) throws TransformerConfigurationException {
        notNull(xslt, "xslt");
        XsltBuilder answer = new XsltBuilder();
        answer.setTransformerSource(xslt);
        return answer;
    }

    /**
     * Creates an XSLT processor using the given XSLT source
     */
    public static XsltBuilder xslt(File xslt) throws TransformerConfigurationException {
        notNull(xslt, "xslt");
        return xslt(new StreamSource(xslt));
    }

    /**
     * Creates an XSLT processor using the given XSLT source
     */
    public static XsltBuilder xslt(URL xslt) throws TransformerConfigurationException, IOException {
        notNull(xslt, "xslt");
        return xslt(xslt.openStream());
    }

    /**
     * Creates an XSLT processor using the given XSLT source
     */
    public static XsltBuilder xslt(InputStream xslt) throws TransformerConfigurationException, IOException {
        notNull(xslt, "xslt");
        return xslt(new StreamSource(xslt));
    }

    /**
     * Sets the output as being a byte[]
     */
    public XsltBuilder outputBytes() {
        setResultHandlerFactory(new StreamResultHandlerFactory());
        return this;
    }

    /**
     * Sets the output as being a String
     */
    public XsltBuilder outputString() {
        setResultHandlerFactory(new StringResultHandlerFactory());
        return this;
    }

    /**
     * Sets the output as being a DOM
     */
    public XsltBuilder outputDOM() {
        setResultHandlerFactory(new DomResultHandlerFactory());
        return this;
    }

    /**
     * Sets the output as being a File where the filename
     * must be provided in the {@link Exchange#XSLT_FILE_NAME} header.
     */
    public XsltBuilder outputFile() {
        setResultHandlerFactory(new FileResultHandlerFactory());
        return this;
    }

    /**
     * Should the output file be deleted when the {@link Exchange} is done.
     * <p/>
     * This option should only be used if you use {@link #outputFile()} as well.
     */
    public XsltBuilder deleteOutputFile() {
        this.deleteOutputFile = true;
        return this;
    }

    public XsltBuilder parameter(String name, Object value) {
        parameters.put(name, value);
        return this;
    }

    /**
     * Sets a custom URI resolver to be used
     */
    public XsltBuilder uriResolver(URIResolver uriResolver) {
        setUriResolver(uriResolver);
        return this;
    }

    /**
     * Enables to allow using StAX.
     * <p/>
     * When enabled StAX is preferred as the first choice as {@link Source}.
     */
    public XsltBuilder allowStAX() {
        setAllowStAX(true);
        return this;
    }

    // Properties
    // -------------------------------------------------------------------------

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setTemplate(Templates template) {
        this.template = template;
    }
    
    public Templates getTemplate() {
        return template;
    }

    public boolean isFailOnNullBody() {
        return failOnNullBody;
    }

    public void setFailOnNullBody(boolean failOnNullBody) {
        this.failOnNullBody = failOnNullBody;
    }

    public ResultHandlerFactory getResultHandlerFactory() {
        return resultHandlerFactory;
    }

    public void setResultHandlerFactory(ResultHandlerFactory resultHandlerFactory) {
        this.resultHandlerFactory = resultHandlerFactory;
    }

    public boolean isAllowStAX() {
        return allowStAX;
    }

    public void setAllowStAX(boolean allowStAX) {
        this.allowStAX = allowStAX;
    }

    /**
     * Sets the XSLT transformer from a Source
     *
     * @param source  the source
     * @throws TransformerConfigurationException is thrown if creating a XSLT transformer failed.
     */
    public void setTransformerSource(Source source) throws TransformerConfigurationException {
        TransformerFactory factory = converter.getTransformerFactory();
        factory.setErrorListener(errorListener);
        if (getUriResolver() != null) {
            factory.setURIResolver(getUriResolver());
        }

        // Check that the call to newTemplates() returns a valid template instance.
        // In case of an xslt parse error, it will return null and we should stop the
        // deployment and raise an exception as the route will not be setup properly.
        Templates templates = factory.newTemplates(source);
        if (templates != null) {
            setTemplate(templates);
        } else {
            throw new TransformerConfigurationException("Error creating XSLT template. "
                    + "This is most likely be caused by a XML parse error. "
                    + "Please verify your XSLT file configured.");
        }
    }

    /**
     * Sets the XSLT transformer from a File
     */
    public void setTransformerFile(File xslt) throws TransformerConfigurationException {
        setTransformerSource(new StreamSource(xslt));
    }

    /**
     * Sets the XSLT transformer from a URL
     */
    public void setTransformerURL(URL url) throws TransformerConfigurationException, IOException {
        notNull(url, "url");
        setTransformerInputStream(url.openStream());
    }

    /**
     * Sets the XSLT transformer from the given input stream
     */
    public void setTransformerInputStream(InputStream in) throws TransformerConfigurationException, IOException {
        notNull(in, "InputStream");
        setTransformerSource(new StreamSource(in));
    }

    public XmlConverter getConverter() {
        return converter;
    }

    public void setConverter(XmlConverter converter) {
        this.converter = converter;
    }

    public URIResolver getUriResolver() {
        return uriResolver;
    }

    public void setUriResolver(URIResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    public boolean isDeleteOutputFile() {
        return deleteOutputFile;
    }

    public void setDeleteOutputFile(boolean deleteOutputFile) {
        this.deleteOutputFile = deleteOutputFile;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    /**
     * Checks whether we need an {@link InputStream} to access the message body.
     * <p/>
     * Depending on the content in the message body, we may not need to convert
     * to {@link InputStream}.
     *
     * @param exchange the current exchange
     * @return <tt>true</tt> to convert to {@link InputStream} beforehand converting to {@link Source} afterwards.
     */
    protected boolean isInputStreamNeeded(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body == null) {
            return false;
        }

        if (body instanceof Source) {
            return false;
        } else if (body instanceof String) {
            return false;
        } else if (body instanceof Document) {
            return false;
        }

        // yes an input stream is needed
        return true;
    }

    /**
     * Converts the inbound body to a {@link Source}, if the body is <b>not</b> already a {@link Source}.
     * <p/>
     * This implementation will prefer to source in the following order:
     * <ul>
     *   <li>StAX - Is StAX is allowed</li>
     *   <li>SAX - SAX as 2nd choice</li>
     *   <li>Stream - Stream as 3rd choice</li>
     *   <li>DOM - DOM as 4th choice</li>
     * </ul>
     */
    protected Source getSource(Exchange exchange, Object body) {
        // body may already be a source
        if (body instanceof Source) {
            return (Source) body;
        }

        Source source = null;
        if (isAllowStAX()) {
            source = exchange.getContext().getTypeConverter().convertTo(StAXSource.class, exchange, body);
        }
        if (source == null) {
            // then try SAX
            source = exchange.getContext().getTypeConverter().convertTo(SAXSource.class, exchange, body);
        }
        if (source == null) {
            // then try stream
            source = exchange.getContext().getTypeConverter().convertTo(StreamSource.class, exchange, body);
        }
        if (source == null) {
            // and fallback to DOM
            source = exchange.getContext().getTypeConverter().convertTo(DOMSource.class, exchange, body);
        }
        if (source == null) {
            if (isFailOnNullBody()) {
                throw new ExpectedBodyTypeException(exchange, Source.class);
            } else {
                try {
                    source = converter.toDOMSource(converter.createDocument());
                } catch (ParserConfigurationException e) {
                    throw new RuntimeTransformException(e);
                }
            }
        }
        return source;
    }

    /**
     * Configures the transformer with exchange specific parameters
     */
    protected void configureTransformer(Transformer transformer, Exchange exchange) {
        if (uriResolver == null) {
            uriResolver = new XsltUriResolver(exchange.getContext().getClassResolver(), null);
        }
        transformer.setURIResolver(uriResolver);
        transformer.setErrorListener(new XmlErrorListener());

        transformer.clearParameters();

        addParameters(transformer, exchange.getProperties());
        addParameters(transformer, exchange.getIn().getHeaders());
        addParameters(transformer, getParameters());

        transformer.setParameter("exchange", exchange);
        transformer.setParameter("in", exchange.getIn());
        transformer.setParameter("out", exchange.getOut());
    }

    protected void addParameters(Transformer transformer, Map<String, Object> map) {
        Set<Map.Entry<String, Object>> propertyEntries = map.entrySet();
        for (Map.Entry<String, Object> entry : propertyEntries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                LOG.trace("Transformer set parameter {} -> {}", key, value);
                transformer.setParameter(key, value);
            }
        }
    }

    private static final class XsltBuilderOnCompletion extends SynchronizationAdapter {
        private final String fileName;

        private XsltBuilderOnCompletion(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void onDone(Exchange exchange) {
            FileUtil.deleteFile(new File(fileName));
        }

        @Override
        public String toString() {
            return "XsltBuilderOnCompletion";
        }
    }

}