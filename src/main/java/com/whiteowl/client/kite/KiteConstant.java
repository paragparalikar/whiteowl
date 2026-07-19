package com.whiteowl.client.kite;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KiteConstant {

    public static final ObjectMapper JSON = createObjectMapper();

    public static final String API_KEY = "kitefront";
    public static final String USER_AGENT_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
    public static final String SEC_CH_UA = "\"Google Chrome\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"";
    public static final String KITE_VERSION = "3.0.0";
    public static final String USER_AGENT_KITE = "kite3-web";
    public static final String WS_VERSION = "3.0.13";

    public static final String URL_WS = "wss://ws.zerodha.com";
    public static final String URL_BASE = System.getProperty("whiteowl.kite.url.base", "https://kite.zerodha.com");
    public static final String URL_INSTRUMENTS = System.getProperty("whiteowl.kite.url.instruments", "https://api.kite.trade/instruments/");
    public static final String URL_BARS = System.getProperty("whiteowl.kite.url.bars", "/oms/instruments/historical");
    public static final String URL_ORDERS = System.getProperty("whiteowl.kite.url.orders", "/oms/orders");
    public static final String URL_POSITIONS = System.getProperty("whiteowl.kite.url.positions", "/oms/portfolio/positions");
    public static final String URL_HOLDINGS = System.getProperty("whiteowl.kite.url.holdings", "/oms/portfolio/holdings");
    public static final String URL_PROFILE = System.getProperty("whiteowl.kite.url.profile", "/oms/user/profile/full");
    public static final String URL_MARGIN = System.getProperty("whiteowl.kite.url.margin", "/oms/user/margins");
    public static final String URL_DASHBOARD = System.getProperty("whiteowl.kite.url.dashboard", "/dashboard");
    public static final String URL_LOGIN = System.getProperty("whiteowl.kite.url.login", "/api/login");
    public static final String URL_TWOFA = System.getProperty("whiteowl.kite.url.twofa", "/api/twofa");
    public static final String URL_QUOTE = System.getProperty("whiteowl.kite.url.quote", "/oms/quote");
    public static final String URL_QUOTE_OHLC = System.getProperty("whiteowl.kite.url.quote.ohlc", "/oms/quote/ohlc");
    public static final String URL_QUOTE_LTP = System.getProperty("whiteowl.kite.url.quote.ltp", "/oms/quote/ltp");
    public static final String URL_GTT = System.getProperty("whiteowl.kite.url.gtt", "/oms/gtt/triggers");

    public static final String FORMAT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT_TIMESTAMP);

    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_ERROR_THRESHOLD = 400;
    public static final String STATUS_SUCCESS = "success";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String ENCTOKEN_PREFIX = "enctoken ";
    public static final String TOTP_FORMAT = "%06d";
    public static final int TOTP_WAIT_LOWER = 27;
    public static final int TOTP_WAIT_UPPER_30 = 29;
    public static final int TOTP_WAIT_UPPER_60 = 59;
    public static final int SECONDS_30 = 30;
    public static final int SECONDS_60 = 60;

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.setDateFormat(new SimpleDateFormat(FORMAT_TIMESTAMP));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

}
