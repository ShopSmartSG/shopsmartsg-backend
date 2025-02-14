package sg.edu.nus.iss.shopsmart_backend.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
public class RedisConfig extends Constants {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${"+REDIS_HOST_KEY+"}")
    private String redisHost;

    @Value("${"+REDIS_PORT_KEY+"}")
    private int redisPort;

    @Value("${"+REDIS_PASSWORD_KEY+"}")
    private String redisPassword;

    @Value("${"+REDIS_DB_NO_KEY+"}")
    private int redisDb;

    @Value("${"+REDIS_USE_SSL+"}")
    private boolean useRedisSsl;

    @Value("${"+REDIS_SSL_CA_CERT_PATH+"}")
    private String sslCaCertPath;

    @Bean
    public JedisPool jedisPool() {
        log.info("Creating JedisPool with host: {}, port: {}, db: {}, password : {}, useRedisSsl: {} and sslCACertPath: {}",
                redisHost, redisPort, redisDb, redisPassword, useRedisSsl, sslCaCertPath);
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        SSLSocketFactory sslSocketFactory = null;
        SSLParameters sslParameters = null;
        HostnameVerifier hostnameVerifier = null;
        if(useRedisSsl){
            try{
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                try (InputStream caInput = new FileInputStream(sslCaCertPath)) {
                    Certificate ca = cf.generateCertificate(caInput);
                    log.info("Loaded CA cert: {}", ((X509Certificate) ca).getSubjectX500Principal());

                    // Create a KeyStore containing our trusted CA.
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("redis-ca", ca);

                    // Create a TrustManager that trusts the CA in our KeyStore.
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(keyStore);

                    // Create an SSLContext that uses our TrustManager.
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
                    sslSocketFactory = sslContext.getSocketFactory();
                    sslParameters = sslContext.getDefaultSSLParameters();

                    // You can implement proper hostname verification here if needed.
                    /*
                    * this would do a blanket check and strictly ensure that origin and destination hosts match
                    * hostnameVerifier as the value of HttpsURLConnection.getDefaultHostnameVerifier()
                    */
                    hostnameVerifier = (hostname, session) -> {
                        if (redisHost.equalsIgnoreCase(hostname)) {
                            return true;
                        }
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
                    };
                    return new JedisPool(poolConfig, redisHost, redisPort, 2000, null, redisDb, true, sslSocketFactory, sslParameters, hostnameVerifier);
                }
            }catch (Exception e){
                log.error("Exception occurred while creating JedisPool with SSL enabled: {}", e.getMessage());
                return null;
            }
        }else{
            if (StringUtils.isEmpty(redisPassword)) {
                log.info("did not find redis password, so skipping it");
                return new JedisPool(poolConfig, redisHost, redisPort, 2000,null, redisDb);
            } else {
                return new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword, redisDb);
            }
        }
    }
}
