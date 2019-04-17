package one.dastec.templogger;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
public class MqttService {

    private IMqttClient publisher;
    private AtomicBoolean connected = new AtomicBoolean(false);

    @Value("${temp-logger.mqtt-server.server-uri}")
    private String serverUri;

    @Value("${temp-logger.mqtt-server.username}")
    private String mqttUser;

    @Value("${temp-logger.mqtt-server.password}")
    private char[] password;

    @PostConstruct
    public void init() throws MqttException {
        String publisherId = UUID.randomUUID().toString();
        publisher = new MqttClient(serverUri,publisherId);
        this.connect();
    }

    public void connect() throws MqttException {

        Thread t = new Thread(() -> {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setUserName(mqttUser);
            options.setPassword(password);
            try {
                publisher.connect(options);
                connected.set(true);
            } catch (MqttException e) {
                log.error(e);
            }
        });
        t.start();
    }

    public void send(Weather weather, String topic){
        if (!connected.get()){
            log.error("Not connected to mqtt server");
        }
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(new WeatherSender(publisher, weather, topic));
        exec.shutdown();
    }
}

class WeatherSender implements Callable<Void> {


    private final IMqttClient client;
    private final String topic;
    private final Weather weather;

    public WeatherSender(IMqttClient client, Weather weather, String topic) {
        this.client = client;
        this.topic = topic;
        this.weather = weather;
    }

    @Override
    public Void call() throws Exception {
        if ( !client.isConnected()) {
            return null;
        }
        MqttMessage msg = new MqttMessage(new Gson().toJson(weather).getBytes());
        msg.setQos(0);
        msg.setRetained(true);
        client.publish(topic,msg);
        return null;
    }

}
