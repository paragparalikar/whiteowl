package com.whiteowl.client.kite;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteDepth;
import com.whiteowl.client.kite.model.KiteMarketDepth;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteTick;
import com.whiteowl.client.kite.model.Order;
import com.whiteowl.core.util.Dates;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KiteWebSocketClient extends WebSocketAdapter implements AutoCloseable {
	private static final long PING_INTERVAL = 2500;
	private static final long RECONNECT_CHECK_DELAY = 500;
	private static final long RECONNECT_CHECK_INTERVAL = 5000;
	private static final String MODE_FULL = "full", MODE_QUOTE = "quote", MODE_LTP = "ltp"; 
	private static final String MESSAGE_SUBSCRIBE = "subscribe", MESSAGE_UNSUBSCRIBE = "unsubscribe", MESSAGE_SET_MODE = "mode";

	private volatile WebSocket webSocket;
	private final KiteSession kiteSession;
	private final ScheduledFuture<?> scheduledFuture;
	private final AtomicLong pongTimestamp = new AtomicLong();
	private final Set<Consumer<Order>> orderConsumers = new HashSet<>();
	private final Map<Long, KiteQuoteMode> quoteModes = new HashMap<>();
	private final Map<Long, Set<Consumer<KiteTick>>> tickConsumers = new HashMap<>();
	private final KiteBinaryMessageParser kiteBinaryMessageParser = new KiteBinaryMessageParser();
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	
	public KiteWebSocketClient(@NonNull final KiteSession kiteSession) {
		this.kiteSession = kiteSession;
		scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::reconnect, 
				RECONNECT_CHECK_DELAY, RECONNECT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void close() throws Exception {
		if(null != scheduledFuture) {
			scheduledFuture.cancel(true);
		}
		disconnect();
		tickConsumers.clear();
		orderConsumers.clear();
	}
	
	@SneakyThrows
	private void connect() {
		disconnect();
		final String uri = createUri();
		pongTimestamp.set(System.currentTimeMillis());
		webSocket = new WebSocketFactory().createSocket(uri);
		webSocket.setPingInterval(PING_INTERVAL);
		webSocket.setPongInterval(PING_INTERVAL);
		webSocket.addListener(this);
		webSocket.connect();
	}
	
	private void disconnect() {
		if(webSocket != null) {
			if(webSocket.isOpen()) {
				webSocket.disconnect();
			}
			webSocket = null;
		}
	}
	
	private boolean isConnected() {
		return null != webSocket 
				&& webSocket.isOpen()
				&& System.currentTimeMillis() - pongTimestamp.get() < 2 * PING_INTERVAL;
	}
	
	private synchronized void reconnect() {
		try {
			if(!isConnected()) {
				log.info("Kite websocket is not connected, trying to connect");
				connect();
				log.info("Kite websocket connected");
			}
		} catch(Exception e) {
			log.error("Error while reconnecting", e);		
		}
	}
	
	private String createUri() throws UnsupportedEncodingException {
		final String enctoken = URLEncoder.encode(kiteSession.getEncToken(), "UTF-8");
		final String username = kiteSession.getCredentials().getUsername();
		return KiteConstant.URL_WS + "/?api_key=" + KiteConstant.APIKEY + 
    			"&user_id=" + username + "&enctoken=" + enctoken +
    			"&uid=" + new Date().getTime() + "&user-agent=" + KiteConstant.USER_AGENT_KITE +
    			"&version=" + KiteConstant.WS_VERSION;
	}
    
	public void subscribeTickListener(
			@NonNull final Collection<Instrument> instruments, 
			@NonNull final KiteQuoteMode kiteQuoteMode,
			@NonNull final Consumer<KiteTick> tickConsumer) {
		reconnect();
		final Collection<Long> alreadySubscribed = new ArrayList<>(tickConsumers.keySet());
		for(Instrument instrument : instruments) {
			quoteModes.put(instrument.getInstrumentToken(), kiteQuoteMode);
			tickConsumers.computeIfAbsent(instrument.getInstrumentToken(), key -> new HashSet<>()).add(tickConsumer);
		}
		final Collection<Long> toSubscribe = new ArrayList<>(tickConsumers.keySet());
		toSubscribe.removeAll(alreadySubscribed);
		webSocket.sendText(createTickerMessagge(toSubscribe, MESSAGE_SUBSCRIBE));
		webSocket.sendText(createModeMessage(toSubscribe, kiteQuoteMode));
	}
	
	@SneakyThrows
	private String createTickerMessagge(final Collection<Long> tokens, final String action) {
        final JSONObject json = new JSONObject();
        final JSONArray array = new JSONArray();
        tokens.forEach(array::put);
        json.put("v", array);
        json.put("a", action);
        return json.toString();
    }
	
	@SneakyThrows
	private String createModeMessage(final Collection<Long> tokens, final KiteQuoteMode mode) {
		final JSONObject json = new JSONObject();
        final JSONArray list = new JSONArray();
        final JSONArray listMain = new JSONArray();
        listMain.put(0, resolveKiteQuoteMode(mode));
        tokens.forEach(list::put);
        listMain.put(1, list);
        json.put("a", MESSAGE_SET_MODE);
        json.put("v", listMain);
        return json.toString();
	}
	
	private String resolveKiteQuoteMode(final KiteQuoteMode mode) {
		switch(mode) {
		case FULL: return MODE_FULL;
		case OHLC: return MODE_QUOTE;
		case LTP: return MODE_LTP;
		default: return MODE_FULL;
		}
	}
	
	public void unsubscribeTickListener(@NonNull final Consumer<KiteTick> tickConsumer) {
		tickConsumers.values().forEach(tickConsumers -> tickConsumers.remove(tickConsumer));
		final Collection<Long> toUnsubscribe = new HashSet<>();
		final Iterator<Entry<Long, Set<Consumer<KiteTick>>>> iterator = tickConsumers.entrySet().iterator();
		while(iterator.hasNext()) {
			final Entry<Long, Set<Consumer<KiteTick>>> entry = iterator.next();
			if(entry.getValue().isEmpty()) {
				toUnsubscribe.add(entry.getKey());
				iterator.remove();
			}
		}
		webSocket.sendText(createTickerMessagge(toUnsubscribe, MESSAGE_UNSUBSCRIBE));
	}
	
	public void subscribeOrderListener(@NonNull final Consumer<Order> orderConsumer) {
		this.orderConsumers.add(orderConsumer);
	}
	
	public void unsubscribeOrderListener(Consumer<Order> orderConsumer) {
		this.orderConsumers.remove(orderConsumer);
	}
	
	@Override
	public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
		pongTimestamp.set(System.currentTimeMillis());
	}
    
	@Override
	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, 
			WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
		if(closedByServer) reconnect();
	}
	
	@Override
	public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
		log.error("Error in websocket connection", cause);
	}
	
	@Override
	public void onTextMessage(WebSocket websocket, String text) throws Exception {
		final JSONObject data = new JSONObject(text);
        if(!data.has("type")) return;
        final String type = data.getString("type");
        if(type.equals("order")) {
        	final Order order = KiteConstant.JSON.readValue(data.getJSONObject("data").toString(), Order.class);
        	orderConsumers.forEach(orderConsumer -> orderConsumer.accept(order));
        } else if(type.equals("error")) {
        	log.error(data.getString("data"));
        } else if(type.equals("instruments_meta")) { 
        	// noop
        } else {
        	log.error("Unknown \"type\" recevied from kite : {}, data : {}", type, data.getString("data"));
        }
	}
	
	@Override
	public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
		final Collection<KiteTick> ticks = kiteBinaryMessageParser.parseBinary(binary);
		for(KiteTick tick : ticks) {
			for(Consumer<KiteTick> consumer : tickConsumers.getOrDefault(tick.getToken(), Collections.emptySet())) {
				consumer.accept(tick);
			}
		}
	}
	
}

class KiteBinaryMessageParser {
	@SuppressWarnings("unused")
	private static final int SEGMENT_NSE_CM = 1, SEGMENT_NSE_FO = 2, SEGMENT_NSE_CD = 3, SEGMENT_BSE_CM = 4,
            SEGMENT_BSE_FO = 5, SEGMENT_BSE_CD = 6, SEGMENT_MCX_FO = 7, SEGMENT_MCX_SX = 8, SEGMENT_INDICES = 9;
	private static final String MODE_FULL = "full", MODE_QUOTE = "quote", MODE_LTP = "ltp"; 
    
    Collection<KiteTick> parseBinary(final byte [] binaryPackets) {
        ArrayList<KiteTick> ticks = new ArrayList<KiteTick>();
        ArrayList<byte[]> packets = splitPackets(binaryPackets);
        for (int i = 0; i < packets.size(); i++) {
            byte[] bin = packets.get(i);
            byte[] t = Arrays.copyOfRange(bin, 0, 4);
            int x = ByteBuffer.wrap(t).getInt();

            //int token = x >> 8;
            int segment = x & 0xff;

            int dec1 = (segment == SEGMENT_NSE_CD) ? 10000000 : (segment == SEGMENT_BSE_CD)? 10000 : 100;

            if(bin.length == 8) {
                KiteTick tick = getLtpQuote(bin, x, dec1, segment != SEGMENT_INDICES);
                ticks.add(tick);
            }else if(bin.length == 28 || bin.length == 32) {
                KiteTick tick = getIndeciesData(bin, x, segment != SEGMENT_INDICES);
                ticks.add(tick);
            }else if(bin.length == 44) {
                KiteTick tick = getQuoteData(bin, x, dec1, segment != SEGMENT_INDICES);
                ticks.add(tick);
            } else if(bin.length == 184) {
                KiteTick tick = getQuoteData(bin, x, dec1, segment != SEGMENT_INDICES);
                tick.setMode(MODE_FULL);
                ticks.add(getFullData(bin, dec1, tick));
            }
        }
        return ticks;
    }
	
    private KiteTick getIndeciesData(byte[] bin, int x, boolean tradable){
        int dec = 100;
        KiteTick tick = new KiteTick();
        tick.setMode(MODE_QUOTE);
        tick.setTradable(tradable);
        tick.setToken(x);
        double lastTradedPrice = convertToDouble(getBytes(bin, 4, 8)) / dec;
        tick.setLastTradedPrice(lastTradedPrice);
        tick.setHighPrice(convertToDouble(getBytes(bin, 8, 12)) / dec);
        tick.setLowPrice(convertToDouble(getBytes(bin, 12, 16)) / dec);
        tick.setOpenPrice(convertToDouble(getBytes(bin, 16, 20)) / dec);
        double closePrice = convertToDouble(getBytes(bin, 20, 24)) / dec;
        tick.setClosePrice(closePrice);
        // here exchange is sending absolute value, hence we change that to %change
        //tick.setNetPriceChangeFromClosingPrice(convertToDouble(getBytes(bin, 24, 28)) / dec);
        setChangeForTick(tick, lastTradedPrice, closePrice);
        if(bin.length > 28) {
            tick.setMode(MODE_FULL);
            long tickTimeStamp = convertToLong(getBytes(bin, 28, 32)) * 1000;
            if(isValidDate(tickTimeStamp)) {
                tick.setTickTimestamp(Dates.toLocalDateTime(tickTimeStamp));
            } else {
                tick.setTickTimestamp(null);
            }
        }
        return tick;
    }

    private KiteTick getLtpQuote(final byte[] bin, final int x, final int dec1, final boolean tradable){
        final KiteTick tick = new KiteTick();
        tick.setMode(MODE_LTP);
        tick.setTradable(tradable);
        tick.setToken(x);
        tick.setLastTradedPrice(convertToDouble(getBytes(bin, 4, 8)) / dec1);
        return tick;
    }
    
    private boolean isValidDate(final long date) {
        try {
        	if(date <= 0) return false;
            final Calendar calendar = Calendar.getInstance();
            calendar.setLenient(false);
            calendar.setTimeInMillis(date);
            calendar.getTime();
            return  true;
        } catch (Exception e) {
            return false;
        }
    }

    private KiteTick getQuoteData(final byte[] bin, final int x, final int dec1, final boolean tradable){
        final KiteTick tick = new KiteTick();
        tick.setMode(MODE_QUOTE);
        tick.setToken(x);
        tick.setTradable(tradable);
        final double lastTradedPrice = convertToDouble(getBytes(bin, 4, 8)) / dec1;
        tick.setLastTradedPrice(lastTradedPrice);
        tick.setLastTradedQuantity(convertToDouble(getBytes(bin, 8, 12)));
        tick.setAverageTradePrice(convertToDouble(getBytes(bin, 12, 16)) / dec1);
        tick.setVolumeTradedToday(convertToLong(getBytes(bin, 16, 20)));
        tick.setTotalBuyQuantity(convertToDouble(getBytes(bin, 20, 24)));
        tick.setTotalSellQuantity(convertToDouble(getBytes(bin, 24, 28)));
        tick.setOpenPrice(convertToDouble(getBytes(bin, 28, 32)) / dec1);
        tick.setHighPrice(convertToDouble(getBytes(bin, 32, 36)) / dec1);
        tick.setLowPrice(convertToDouble(getBytes(bin, 36, 40)) / dec1);
        final double closePrice = convertToDouble(getBytes(bin, 40, 44)) / dec1;
        tick.setClosePrice(closePrice);
        setChangeForTick(tick, lastTradedPrice, closePrice);
        return tick;
    }

    private void setChangeForTick(final KiteTick tick, final double lastTradedPrice, final double closePrice){
    	tick.setChange(0 == closePrice ? 0 : (lastTradedPrice - closePrice) * 100 / closePrice);
    }

    private KiteTick getFullData(final byte[] bin, final int dec, final KiteTick tick){
        final long lastTradedtime = convertToLong(getBytes(bin, 44, 48)) * 1000;
        tick.setLastTradedTime(isValidDate(lastTradedtime) ? Dates.toLocalDateTime(lastTradedtime) : null);
        tick.setOi(convertToDouble(getBytes(bin, 48, 52)));
        tick.setOiDayHigh(convertToDouble(getBytes(bin, 52, 56)));
        tick.setOiDayLow(convertToDouble(getBytes(bin, 56, 60)));
        final long tickTimeStamp = convertToLong(getBytes(bin, 60, 64)) * 1000;
        tick.setTickTimestamp(isValidDate(tickTimeStamp) ? Dates.toLocalDateTime(tickTimeStamp) : null);
        tick.setDepth(getDepthData(bin, dec, 64, 184));
        return  tick;
    }

    private KiteMarketDepth getDepthData(final byte[] bin, final int dec, final int start, final int end){
    	final KiteMarketDepth kiteMarketDepth = new KiteMarketDepth();
        final byte[] depthBytes = getBytes(bin, start, end);
        int s = 0;
        final ArrayList<KiteDepth> buy = new ArrayList<KiteDepth>();
        final ArrayList<KiteDepth> sell = new ArrayList<KiteDepth>();
        for (int k = 0; k < 10; k++) {
            s = k * 12;
            KiteDepth depth = new KiteDepth();
            depth.setQuantity((int)convertToDouble(getBytes(depthBytes, s, s + 4)));
            depth.setPrice(convertToDouble(getBytes(depthBytes, s + 4, s + 8))/dec);
            depth.setOrders((int)convertToDouble(getBytes(depthBytes, s + 8, s + 10)));

            if (k < 5) {
                buy.add(depth);
            } else {
                sell.add(depth);
            }
        }
        kiteMarketDepth.setBuy(buy);
        kiteMarketDepth.setSell(sell);
        return kiteMarketDepth;
    }

    private ArrayList<byte []> splitPackets(final byte[] bin){
        final ArrayList<byte []> packets = new ArrayList<byte []>();
        final int noOfPackets = getLengthFromByteArray(getBytes(bin, 0, 2)); //in.read(bin, 0, 2);
        int j = 2;
        for(int i = 0; i < noOfPackets; i++){
            int sizeOfPacket = getLengthFromByteArray(getBytes(bin, j, j + 2));//in.read(bin, j, j+2);
            byte[] packet = Arrays.copyOfRange(bin, j + 2, j + 2 + sizeOfPacket);
            packets.add(packet);
            j = j + 2 + sizeOfPacket;
        }
        return packets;
    }

    private byte[] getBytes(final byte[] bin, final int start, final int end){
        return Arrays.copyOfRange(bin, start, end);
    }

    private double convertToDouble(final byte[] bin){
        final ByteBuffer bb = ByteBuffer.wrap(bin);
        bb.order(ByteOrder.BIG_ENDIAN);
        if(bin.length < 4)
            return bb.getShort();
        else if(bin.length < 8)
            return bb.getInt();
        else
            return bb.getDouble();
    }

    private long convertToLong(final byte[] bin){
        final ByteBuffer bb = ByteBuffer.wrap(bin);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();
    }

    private int getLengthFromByteArray(final byte[] bin){
        final ByteBuffer bb = ByteBuffer.wrap(bin);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort();
    }
    
}

