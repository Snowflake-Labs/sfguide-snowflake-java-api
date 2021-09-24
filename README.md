# A Simple API Powered by Snowflake

Technologies used: [Snowflake](https://snowflake.com/), [Java](https://www.java.com/), [AWS API Gateway](https://aws.amazon.com/api-gateway/), [AWS Lambda](https://aws.amazon.com/lambda/), [Serverless Framework](https://www.serverless.com/)

Requirements: 
* Snowflake.com and Serverless.com account
* Java jdk and maven installed
* Citibike data loaded into Snowflake
* Snowflake user authorized to access citibike data with key pair authentication

This project demonstrates how to build and deploy a custom API powered by Snowflake. It uses a simple Java API service running on AWS Lambda using Serverless Framework. Connectivity to Snowflake is made via key pair authentication.

## Configuration

Copy the serverless-template.yml to serverless.yml and modify the parameters according to your Snowflake configuration. Put your private key
to your Snowflake user in AWS SSM is us-west-2 region under the parameter <ACCOUNT>.DATA_APPS_DEMO.

Install serverless and configure serverless (sls) for the project.

```bash
sls login
```

## Deployment

Build and deploy the application to AWS. For your first time, you will have to run sls without deploy to configure the project.

```bash
mvn package
sls deploy
```

### Invocation

After successful deployment, you can call the created application via HTTP:

```bash
curl https://xxxxxxx.execute-api.us-west-2.amazonaws.com/dev/
```

Which should result in the following response:

```json
{"result":"Nothing to see here", "time_ms": 0}
```

## Scaling

By default, AWS Lambda limits the total concurrent executions across all functions within a given region to 1000. The default limit is a safety limit that protects you from costs due to potential runaway or recursive functions during initial development and testing. To increase this limit above the default, follow the steps in [To request a limit increase for concurrent executions](http://docs.aws.amazon.com/lambda/latest/dg/concurrent-executions.html#increase-concurrent-executions-limit).