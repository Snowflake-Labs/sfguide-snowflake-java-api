package com.snowflake;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayList;
import java.security.PrivateKey;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import org.apache.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;


public class Handler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = Logger.getLogger(Handler.class);
	private static final Map<String, String> HEADERS = new HashMap<>();
	static {
		HEADERS.put("Content-Type", "application/json");
	}

	private static class Connector
	{
		private static Connection conn = null;

		private static class PrivateKeyReader
		{
			private static PrivateKey get(String key) throws Exception
			{
				Security.addProvider(new BouncyCastleProvider());
				PEMParser pemParser = new PEMParser(new StringReader(key));
				PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
				pemParser.close();
				JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
				return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
			}
		}
		
		public static Connection connect() throws Exception {
			if (conn == null) {
				Map<String,String> env = System.getenv();
				Properties props = new Properties();
				props.put("CLIENT_SESSION_KEEP_ALIVE", true);
				props.put("account", env.get("SNOWFLAKE_ACCOUNT"));
				props.put("user", env.get("SNOWFLAKE_USER"));
				props.put("privateKey", PrivateKeyReader.get(env.get("SNOWFLAKE_PRIVATE_KEY")));
				props.put("warehouse", env.get("SNOWFLAKE_WAREHOUSE"));
				props.put("db", env.get("SNOWFLAKE_DATABASE"));
				props.put("schema", env.get("SNOWFLAKE_SCHEMA"));
				String url = "jdbc:snowflake://" + env.get("SNOWFLAKE_ACCOUNT") + ".snowflakecomputing.com/";
				return DriverManager.getConnection(url, props);
			}
			return conn;
		}
	}

	private ApiGatewayResponse handleDefault() {
		return ApiGatewayResponse.builder()
		.setStatusCode(200)
		.setObjectBody(new DefaultResponse())
		.setHeaders(HEADERS)
		.build();
	}

	private PreparedStatement monthlyPreparedStatement(Map<String, String> queryStringParameters, Connection conn) throws Exception {
		PreparedStatement stat;
		if (queryStringParameters != null && queryStringParameters.get("start_range") != null && queryStringParameters.get("end_range") != null) {
			String sql = "select COUNT(*) as trip_count, MONTHNAME(starttime) as month from demo.trips where starttime between ? and ? group by MONTH(starttime), MONTHNAME(starttime) order by MONTH(starttime);";	
			stat = conn.prepareStatement(sql);
			stat.setString(1, queryStringParameters.get("start_range"));
			stat.setString(2, queryStringParameters.get("end_range"));
			return stat;
		}
		String sql = "select COUNT(*) as trip_count, MONTHNAME(starttime) as month from demo.trips group by MONTH(starttime), MONTHNAME(starttime) order by MONTH(starttime);";
		stat = conn.prepareStatement(sql);
		return stat;
	}

	private PreparedStatement dayOfWeekPreparedStatement(Map<String, String> queryStringParameters, Connection conn) throws Exception {
		PreparedStatement stat;
		if (queryStringParameters != null && queryStringParameters.get("start_range") != null && queryStringParameters.get("end_range") != null) {
			String sql = "select COUNT(*) as trip_count, DAYNAME(starttime) as day_of_week from demo.trips where starttime between ? and ? group by DAYOFWEEK(starttime), DAYNAME(starttime) order by DAYOFWEEK(starttime);";						
			stat = conn.prepareStatement(sql);
			stat.setString(1, queryStringParameters.get("start_range"));
			stat.setString(2, queryStringParameters.get("end_range"));
			return stat;
		}
		String sql = "select COUNT(*) as trip_count, DAYNAME(starttime) as day_of_week from demo.trips group by DAYOFWEEK(starttime), DAYNAME(starttime) order by DAYOFWEEK(starttime);";
		stat = conn.prepareStatement(sql);
		return stat;
	}

	private PreparedStatement temperaturePreparedStatement(Map<String, String> queryStringParameters, Connection conn) throws Exception {
		PreparedStatement stat;
		if (queryStringParameters != null && queryStringParameters.get("start_range") != null && queryStringParameters.get("end_range") != null) {
			String sql = "with weather_trips as (select * from demo.trips t inner join demo.weather w on date_trunc(\"day\", t.starttime) = w.observation_date) select round(temp_avg_f, -1) as temp, count(*) as trip_count from weather_trips where starttime between ? and ? group by round(temp_avg_f, -1) order by round(temp_avg_f, -1) asc;";	
			stat = conn.prepareStatement(sql);
			stat.setString(1, queryStringParameters.get("start_range"));
			stat.setString(2, queryStringParameters.get("end_range"));
			return stat;
		}
		String sql = "with weather_trips as (select * from demo.trips t inner join demo.weather w on date_trunc(\"day\", t.starttime) = w.observation_date) select round(temp_avg_f, -1) as temp, count(*) as trip_count from weather_trips group by round(temp_avg_f, -1) order by round(temp_avg_f, -1) asc;";
		stat = conn.prepareStatement(sql);
		return stat;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {		
		String path = (String) input.get("path");
		Map<String, String> queryStringParameters = (Map<String, String>) input.get("queryStringParameters");		
		try {			
			PreparedStatement stat;
			switch(path) {
				case "/trips/monthly":
					Connection conn = Connector.connect();
					stat = monthlyPreparedStatement(queryStringParameters, conn);
					break;
				case "/trips/day_of_week":
					conn = Connector.connect();
					stat = dayOfWeekPreparedStatement(queryStringParameters, conn);
					break;
				case "/trips/temperature":
					conn = Connector.connect();
					stat = temperaturePreparedStatement(queryStringParameters, conn);
					break;
				default:
					return handleDefault();
			}

			long start_time = System.nanoTime();
			ResultSet rs = stat.executeQuery();
			long time_ms = (System.nanoTime() - start_time) / 1000000;
			ArrayList<Object[]> results = new ArrayList<Object[]>();
			while (rs.next()) {
				results.add(new Object[]{rs.getObject(1), rs.getObject(2)});
			}

			return ApiGatewayResponse.builder()
					.setStatusCode(200)
					.setObjectBody(new Response(results, time_ms))
					.setHeaders(HEADERS)
					.build();		
		} catch (Exception e) {
			LOG.error(e);
			return ApiGatewayResponse.builder()
			.setStatusCode(500)
			.build();
		}
	}
}
