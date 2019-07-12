package cn.micro.lemon.filter;

import cn.micro.lemon.LemonConfig;
import cn.micro.lemon.LemonStatusCode;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Lemon Context
 *
 * @author lry
 */
@Data
@Slf4j
@ToString
public class LemonContext {

    public final static String LEMON_ID = "Lemon-Id";
    private final static String LEMON_TIME = "Lemon-Time";
    private final static String LEMON_CODE_KEY = "Lemon-Code";
    private final static String LEMON_CODE_MESSAGE = "Lemon-Message";
    private final static String APPLICATION_JSON = "application/json;charset=UTF-8";

    private String id;
    private long startTime;
    private long sendTime;
    private long receiveTime;
    private long endTime;

    private String path;
    private String uri;
    private String method;
    private boolean keepAlive;
    private int contentLength;

    private final List<String> paths = new ArrayList<>();
    private final Map<String, String> headers = new HashMap<>();
    private final List<Map.Entry<String, String>> headerAll = new ArrayList<>();
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, List<String>> parameterAll = new HashMap<>();

    private byte[] contentByte;
    private String content;

    private LemonConfig lemonConfig;
    private ChannelHandlerContext ctx;

    private Object result;

    public LemonContext(LemonConfig lemonConfig, ChannelHandlerContext ctx) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.startTime = System.currentTimeMillis();
        this.lemonConfig = lemonConfig;
        this.ctx = ctx;
        MDC.put(LEMON_ID, id);
    }


    /**
     * The add headers
     *
     * @param path path
     */
    public void addPaths(String path) {
        if (path.length() <= 1) {
            return;
        }

        String tempPath = path;
        if (tempPath.startsWith("/")) {
            tempPath = tempPath.substring(1);
        }
        if (tempPath.endsWith("/")) {
            tempPath = tempPath.substring(0, tempPath.length() - 1);
        }

        String[] pathArray = tempPath.split("/");
        if (pathArray.length > 0) {
            paths.addAll(Arrays.asList(pathArray));
        }
    }

    /**
     * The add parameters
     *
     * @param parameterAll {@link Map}
     */
    public void addParameters(Map<String, List<String>> parameterAll) {
        if (parameterAll == null || parameterAll.size() == 0) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : parameterAll.entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.size() == 0) {
                continue;
            }
            parameters.put(entry.getKey(), values.get(0));
        }
    }

    /**
     * The add headers
     *
     * @param headersAll {@link List}
     */
    public void addHeaders(List<Map.Entry<String, String>> headersAll) {
        if (headersAll == null || headersAll.size() == 0) {
            return;
        }
        for (Map.Entry<String, String> entry : headersAll) {
            headers.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * The write and flush
     *
     * @param statusCode {@link LemonStatusCode}
     */
    public void writeAndFlush(LemonStatusCode statusCode) {
        FullHttpResponse response;
        if (LemonStatusCode.SUCCESS != statusCode) {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        } else {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
        }

        int contentLength = 0;
        ByteBuf content = response.content();
        if (content != null) {
            contentLength = content.readableBytes();
        }

        HttpHeaders headers = response.headers();
        headers.set(LEMON_ID, id);
        headers.set(LEMON_TIME, System.currentTimeMillis());
        headers.set(LEMON_CODE_KEY, statusCode.getCode());
        headers.set(LEMON_CODE_MESSAGE, statusCode.getMessage());
        headers.set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
        headers.set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON);
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP_DEFLATE);

        Map<String, Object> resHeaders = lemonConfig.getResHeaders();
        if (resHeaders != null && resHeaders.size() > 0) {
            for (Map.Entry<String, Object> entry : resHeaders.entrySet()) {
                headers.set(entry.getKey(), entry.getValue());
            }
        }


        ctx.writeAndFlush(response).addListener(future -> {
            this.endTime = System.currentTimeMillis();
            log.debug("The writeAndFlush content: {}", content);
            MDC.remove(LEMON_ID);
        });
    }

}
