package com.yinuo.cloud.client.util;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/***********************************************************************
 *
 *  @author liuzuoxian
 *  @date 2019/4/3 15:47
 *  @version 1.0.0
 *
 ***********************************************************************/
public abstract class WebUtil {
    private static final String BOUNDARY = "----Y3JlYXRvci14aWFu";
    private static final String FILE_ENCTYPE = "multipart/form-data";
    private static final String ENTER = "\r\n";

    /**
     * 执行请求
     *
     * @param hostUrl   请求地址
     * @param method    请求方法
     * @param headers   请求头
     * @param params    请求参数
     * @param fileMap   请求文件
     * @param charset   编码
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口
     * @return
     * @throws Exception
     */
    public static String execute(String hostUrl, final String method, Map<String, String> headers,
                                 Map<String, String> params, Map<String, InputStream> fileMap,
                                 String charset, String proxyHost, Integer proxyPort) throws Exception {
        if (isGet(method) && null != params) {
            if (hostUrl.endsWith("?")) {
                hostUrl = hostUrl + paramsToQuery(params);
            } else {
                hostUrl = hostUrl + "?" + paramsToQuery(params);
            }
        }
        URL url = new URL(hostUrl);
        HttpURLConnection connection = genConnection(url, proxyHost, proxyPort);
        if (null != fileMap && fileMap.size() > 0) {
            headers.put("Connection", "Keep-Alive");
            headers.put("Content-Type", FILE_ENCTYPE + "; boundary=" + BOUNDARY);
        } else {
            if (null != headers && isEmpty(headers.get("Content-Type"))) {
                headers.put("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        setConnection(connection, method, headers, charset);
        OutputStream outputStream = connection.getOutputStream();
        DataOutputStream dos = new DataOutputStream(outputStream);
        if (!isGet(method)) {
            boolean isJson = null != headers && headers.get("Content-Type").startsWith("application/json");
            postOrPostFile(dos, params, fileMap, isJson);
            dos.close();
        }
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader bReader = new BufferedReader(inputStreamReader);
            String str;
            StringBuilder builder = new StringBuilder();
            while ((str = bReader.readLine()) != null) {
                builder.append(str);
            }
            return builder.toString();
        } else {
            return "";
        }
    }

    /**
     * 根据请求头等信息设置connection
     *
     * @param con
     * @param method  请求方法
     * @param headers 请求头
     * @param charset 字符编码
     */
    private static void setConnection(HttpURLConnection con, final String method, Map<String, String> headers, String charset) {
        if (null != headers) {
            for (String headKey : headers.keySet()) {
                con.setRequestProperty(headKey, headers.get(headKey));
            }
        }
        try {
            con.setRequestMethod(method);
        } catch (ProtocolException e) {
            e.printStackTrace();
            throw new RuntimeException("http method is error,example GET,POST,ect");
        }
        con.setRequestProperty("Charset", charset);
        con.setDoInput(true);
        con.setDoOutput(true);
    }

    /**
     * 根据URL生成connection
     *
     * @param url       URL
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口
     * @return
     * @throws IOException
     */
    private static HttpURLConnection genConnection(URL url, final String proxyHost, Integer proxyPort) throws IOException {
        HttpURLConnection connection;
        if (!isEmpty(proxyHost) && null != proxyPort && proxyPort > -1) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
            connection = (HttpURLConnection) url.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        int CONNECTION_TIME_OUT = 15000;
        connection.setConnectTimeout(CONNECTION_TIME_OUT);
        int READ_TIME_OUT = 15000;
        connection.setReadTimeout(READ_TIME_OUT);
        return connection;
    }

    /**
     * 字符串是否为空
     *
     * @param str
     * @return
     */
    private static boolean isEmpty(String str) {
        return null == str || "".equals(str.trim());
    }

    /**
     * 是否是get请求
     *
     * @param method
     * @return
     */
    private static boolean isGet(@NotNull String method) {
        return "GET".equals(method);
    }

    /**
     * 请求方式Content-Type: application/x-www-form-urlencoded
     *
     * @param params
     * @return
     */
    private static String paramsToQuery(Map<String, String> params) {
        if (null != params && params.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (String key : params.keySet()) {
                builder.append(key).append("=").append(params.get(key)).append("&");
            }
            return builder.deleteCharAt(builder.length() - 1).toString();
        }
        return "";
    }

    /**
     * 请求方式Content-Type: application/json
     *
     * @param params
     * @return
     */
    private static String mapToJsonString(Map<String, String> params) {
        StringBuilder builder = new StringBuilder("{");
        if (null != params && params.size() > 0) {
            for (String key : params.keySet()) {
                builder.append("\"").append(key).append("\"").append(":").append("\"")
                        .append(params.get(key)).append("\"").append(",");
            }
            return builder.deleteCharAt(builder.length() - 1).append("}").toString();
        }
        return "{}";
    }

    /**
     * post请求或文件上传
     *
     * @param dos
     * @param params
     * @param fileMap
     * @param isJson
     * @throws IOException
     */
    private static void postOrPostFile(OutputStream dos, Map<String, String> params, Map<String, InputStream> fileMap, boolean isJson) throws IOException {
        if (null != fileMap && fileMap.size() > 0) {
            if (null != params) {
                StringBuilder builder = new StringBuilder();
                for (String key : params.keySet()) {
                    builder.append("--").append(BOUNDARY).append(ENTER);
                    builder.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(ENTER).append(ENTER);
                    builder.append(params.get(key)).append(ENTER);
                }
                dos.write(builder.toString().getBytes());
            }
            for (String key : fileMap.keySet()) {
                String builder = "--" + BOUNDARY + ENTER +
                        "Content-Disposition: form-data; name=\"file\"" +
                        "; filename=\"" + key + "\"" + ENTER + ENTER;
                dos.write(builder.getBytes());
                InputStream inputStream = fileMap.get(key);
                byte[] buffer = new byte[1024 * 1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, len);
                }
                dos.write(ENTER.getBytes());
                inputStream.close();
            }
            String builder = "--" + BOUNDARY + "--\r\n";
            dos.write(builder.getBytes());
            dos.flush();
        } else {
            if (null != params) {
                String paramsString;
                if (isJson) {
                    paramsString = mapToJsonString(params);
                } else {
                    paramsString = paramsToQuery(params);
                }
                dos.write(paramsString.getBytes());
                dos.flush();
            }
        }
    }

    /**
     * get请求
     *
     * @param hostUrl
     * @param headers
     * @param params
     * @return
     * @throws Exception
     */
    public static String get(String hostUrl, Map<String, String> headers, Map<String, String> params) throws Exception {
        return execute(hostUrl, "GET", headers, params, null, "UTF-8", null, null);
    }

    /**
     * post请求
     *
     * @param hostUrl
     * @param headers
     * @param params
     * @return
     * @throws Exception
     */
    public static String post(String hostUrl, Map<String, String> headers, Map<String, String> params) throws Exception {
        return execute(hostUrl, "POST", headers, params, null, "UTF-8", null, null);
    }

    /**
     * 文件上传
     *
     * @param hostUrl
     * @param headers
     * @param params
     * @param fileMap
     * @return
     * @throws Exception
     */
    public static String uploadFile(String hostUrl, Map<String, String> headers, Map<String, String> params, Map<String, InputStream> fileMap) throws Exception {
        return execute(hostUrl, "POST", headers, params, fileMap, "UTF-8", null, null);

    }

    public static String get(String hostUrl) throws Exception {
        return get(hostUrl, null, null);
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("account", "root");
        params.put("password", "root");
        String post = post("http://localhost:8081/disk/user/login", null, params);
        System.out.println(post);
    }
}
