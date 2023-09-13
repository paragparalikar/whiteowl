package org.javalite.http;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Put extends Request<Put> {

    private final byte[] content;
    private Map<String, String> params = new HashMap<>();

    /**
     * Constructor for making PUT requests.
     *
     * @param url URL of resource.
     * @param content content to be posted to the resource.
     * @param connectTimeout connection timeout.
     * @param readTimeout read timeout.
     */
    public Put(String url, byte[] content, int connectTimeout, int readTimeout) {
        super(url, connectTimeout, readTimeout);
        this.content = content;
    }

    @Override
    public Put doConnect() {
        try {
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("PUT");
            connection.setInstanceFollowRedirects(redirect);

            if(params.size() > 0){
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            OutputStream out = connection.getOutputStream();
            if(params.size() > 0){
                out.write(Http.map2URLEncoded(params).getBytes());
            }
            if(content != null){
                out.write(content);
            }

            out.flush();
            return this;
        } catch (Exception e) {
            throw new HttpException("Failed URL: " + url, e);
        }
    }


    /**
     * Convenience method to add multiple parameters to the request.
     * <p></p>
     * Names and values alternate: name1, value1, name2, value2, etc.
     *
     * @param namesAndValues names/values of multiple fields to be added to the request.
     * @return self
     */
    public Put params(String ... namesAndValues){

        if(namesAndValues == null ){
            throw new NullPointerException("'names and values' cannot be null");
        }

        if(namesAndValues.length % 2 != 0){
            throw new IllegalArgumentException("mus pas even number of arguments");
        }

        for (int i = 0; i < namesAndValues.length - 1; i += 2) {
            if (namesAndValues[i] == null) throw new IllegalArgumentException("parameter names cannot be nulls");
            params.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return this;
    }

    /**
     * Adds a parameter to the request as in a HTML form.
     *
     * @param name name of parameter
     * @param value value of parameter
     * @return self
     */
    public Put param(String name, String value){
        params.put(name, value);
        return this;
    }

}
