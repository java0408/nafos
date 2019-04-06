package nafos.bootStrap.handle.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import nafos.core.util.CastUtil;
import nafos.core.util.JsonUtil;
import nafos.core.util.ObjectUtil;
import org.apache.commons.beanutils.BeanUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class NsRequest extends BuildHttpObjectAggregator.AggregatedFullHttpRequest {

    private Map<String, String> requestParams;

    private Map<String, Object> bodyParams;

    NsRequest(HttpRequest request, ByteBuf content, HttpHeaders trailingHeaders) {
        super(request, content, trailingHeaders);
    }


    public String stringQueryParam(String key) {
        return requestParams().get(key);
    }

    public int intQueryParam(String key) {
        return CastUtil.castInt(requestParams().get(key));
    }

    public boolean booleanQueryParam(String key) {
        return CastUtil.castBoolean(requestParams().get(key));
    }

    public long longQueryParam(String key) {
        return CastUtil.castLong(requestParams().get(key));
    }

    public Object objectQueryParam(String key) {
        return requestParams().get(key);
    }


    public String stringBodyParam(String key) {
        return CastUtil.castString(bodyParams().get(key));
    }

    public int intBodyParam(String key) {
        return CastUtil.castInt(bodyParams().get(key));
    }

    public boolean booleanBodyParam(String key) {
        return CastUtil.castBoolean(bodyParams().get(key));
    }

    public long longBodyParam(String key) {
        return CastUtil.castLong(bodyParams().get(key));
    }

    public Object objectBodyParam(String key) {
        return bodyParams().get(key);
    }


    /**
     * 获取cookie列表
     *
     * @return
     */
    public Set<Cookie> getCookies() {
        Set<Cookie> cookies = new HashSet<>();
        String cookieStr = headers().get("Cookie");
        if (ObjectUtil.isNotNull(cookieStr)) {
            cookies = ServerCookieDecoder.LAX.decode(cookieStr);
        }
        return cookies;
    }


    /**
     * restful风格的postJSON解析
     */
    private Map<String, ?> bodyParams() {
        if (bodyParams == null) {
            // 处理POST请求
            String strContentType = headers().get("Content-Type");
            strContentType = ObjectUtil.isNotNull(strContentType) ? strContentType.trim() : "";
            if (strContentType.contains("application/json")) {
                bodyParams = getJSONParams();
            } else {
                bodyParams = getFormParams();
            }
        }
        return bodyParams;
    }


    /**
     * 解析from表单数据（Content-Type = x-www-form-urlencoded）,默认格式
     */
    private Map<String, Object> getFormParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), this);
        List<InterfaceHttpData> postData = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : postData) {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                MemoryAttribute attribute = (MemoryAttribute) data;
                params.put(attribute.getName(), attribute.getValue());
            }
        }
        return params;
    }

    /**
     * 解析json数据（Content-Type = application/json）
     */
    private Map<String, Object> getJSONParams() {
        ByteBuf jsonBuf = content();
        String jsonStr = jsonBuf.toString(CharsetUtil.UTF_8);
        return JsonUtil.jsonToMap(jsonStr);
    }


    /**
     * 解析queryparams
     *
     * @return
     */
    private Map<String, String> requestParams() {
        if (requestParams == null) {
            Map<String, String> map = new HashMap<>();
            QueryStringDecoder decoder = new QueryStringDecoder(uri());
            for (Map.Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
                map.put(entry.getKey(), entry.getValue().get(0));
            }
            requestParams = map;
        }
        return requestParams;
    }

    /**
     * xml风格的postJSON解析
     *
     * @param clazz
     * @return
     */
    private Object xmlJsonEncode(Class<?> clazz) {
        Object fieldObj = null;
        Map<String, String> postMap = new HashMap<>();
        HttpPostRequestDecoder httpPostRequestDecoder = new HttpPostRequestDecoder(this);
        httpPostRequestDecoder.offer(this);
        List<InterfaceHttpData> parmList = httpPostRequestDecoder.getBodyHttpDatas();
        for (InterfaceHttpData parm : parmList) {
            Attribute data = (Attribute) parm;
            try {
                postMap.put(data.getName(), data.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!Map.class.isAssignableFrom(clazz)) {
            try {
                BeanUtils.populate(fieldObj, postMap);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            fieldObj = postMap;
        }
        return fieldObj;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(Map<String, String> requestParams) {
        this.requestParams = requestParams;
    }

    public Map<String, Object> getBodyParams() {
        return bodyParams;
    }

    public void setBodyParams(Map<String, Object> bodyParams) {
        this.bodyParams = bodyParams;
    }
}