package se.flinker.logback.extras;


import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.status.ErrorStatus;
import se.flinker.logback.extras.Http.Response;

public class SlackIncomingWebHooksAppender extends OutputStreamAppender<ILoggingEvent> {
    private static final String DEFAULT_COLOR = "#0099ff";
    
    private static final String DISABLED = "DISABLED";
    
    private String url;
    private String username = SlackIncomingWebHooksAppender.class.getSimpleName();
    private String iconEmoji = ":ghost:";
    private String errorColor = "#D00000";
    private String warnColor = "#ffff00";
    private String infoColor = "#0066ff";
    private String debugColor = "#0066ff";
    private String traceColor = "#ff00ff";
    private String channel;
    private boolean async = true;
    
    
    @Override
    public void start() {
        if (isStarted()) {
            return;
        }
        
        int errors = 0;       
        
        if (this.url == null) {
            addStatus(new ErrorStatus("No url set for the appender named \"" + name + "\".", this));
            errors++;
        }
        
        try {
            URI.create(this.url);
        } catch (Exception e) {
            addStatus(new ErrorStatus("Invalid url set for the appender named \"" + name + "\".", this));
            errors++;
        }
        setOutputStream(new ByteArrayOutputStream());
        if (errors == 0){
            super.start();
        }
    }
    
    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        super.stop();
    }
    
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null || !isStarted()) {
            return;
        }
        
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            setOutputStream(os);
            super.append(eventObject);
            lock.lock();
            Map<String, Object> data = new HashMap<>();
            if (nonNull(channel)) {
                data.put("channel", this.channel);
            }
            data.put("username", this.username);
            data.put("icon_emoji", this.iconEmoji);
            
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("fallback", eventObject.getFormattedMessage());
            if (eventObject.getLevel().levelInt == Level.ERROR.levelInt) {
                attachment.put("pretext", "_" + eventObject.getFormattedMessage() + "_");
            }
            attachment.put("color", getColor(eventObject.getLevel()));
            attachment.put("title", eventObject.getLevel().toString());
            attachment.put("text", os.toString());
            attachment.put("mrkdwn_in", asList("pretext"));
            
            data.put("attachments", asList(attachment));
            post(data);
            
        } catch (Exception e) {
            this.started = false;
            addStatus(new ErrorStatus("Some failure in appender", this, e));
        } finally {
            lock.unlock();
        }
    }

    private void post(final Map<String, Object> data) {
        if (!DISABLED.equals(this.url)) {
            
            Thread t = new Thread("thread-" + getClass().getSimpleName()){
                @Override
                public void run() {
                    try {
                        Http http = new Http();
                        String body = format("payload=%s", URLEncoder.encode(new Gson().toJson(data), "utf-8"));
                        Response post = http.post(SlackIncomingWebHooksAppender.this.url, Collections.emptyMap(), body);
                        
                        if (post.code == 500) {
                            addStatus(new ErrorStatus("Failure (http status 500) in appender", SlackIncomingWebHooksAppender.this));
                        }
                    } catch (Exception e) {
                        addStatus(new ErrorStatus("Some failure in appender", SlackIncomingWebHooksAppender.this, e));
                    }
                }
            };
            
            if (async) {
                t.start();
            } else {
                t.run();
            }
        }
    }

    private String getColor(Level level) {
        switch(level.levelInt) {
        case Level.ERROR_INT:
            return errorColor;
        case Level.WARN_INT:
            return warnColor;
        case Level.INFO_INT:
            return infoColor;
        case Level.DEBUG_INT:
            return debugColor;
        case Level.TRACE_INT:
            return traceColor;
        }
        
        return DEFAULT_COLOR;
    }   
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }
    
    public void setErrorColor(String errorColor) {
        this.errorColor = errorColor;
    }

    public void setWarnColor(String warnColor) {
        this.warnColor = warnColor;
    }

    public void setInfoColor(String infoColor) {
        this.infoColor = infoColor;
    }

    public void setDebugColor(String debugColor) {
        this.debugColor = debugColor;
    }

    public void setTraceColor(String traceColor) {
        this.traceColor = traceColor;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
