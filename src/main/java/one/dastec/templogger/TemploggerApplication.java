package one.dastec.templogger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class TemploggerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemploggerApplication.class, args);
    }

}

@RestController
@RequestMapping("")
@Log4j2
class WeatherController {

    private Weather weather = new Weather();
    private Object lock = new Object(){};

    @Autowired
    private MqttService mqttService;

    @Value("${temp-logger.jwt.secret}")
    private String secret;

    @RequestMapping(path = "weather", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.GET)
    public Message getWeather(){
        synchronized (lock){
            return new Message(new Date(), weather);
        }
    }

    @RequestMapping(path = "weather", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.POST)
    public Message postWeather(@RequestBody Weather body, @RequestHeader("Authorization") String authorization){
        DecodedJWT jwt = null;
        String token = authorization.replace("Bearer ","");
        try {
            jwt = JWT.decode(token);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .build();
            verifier.verify(jwt);
        } catch (Exception exception){
            log.error(token);
            throw new ResourceNotFoundException("Invalid Token");
        }
        synchronized (lock){
            this.weather = body;
        }
        log.info("Weather: "+body);
        List<String> audience = jwt.getAudience();
        log.info("Weather: "+body+ " Aud: "+audience);
        if (!audience.isEmpty()) {
            mqttService.send(weather, audience.get(0));
        }
        return new Message(new Date(),weather);
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Message {
    private Date date;
    private Weather weather;
}


