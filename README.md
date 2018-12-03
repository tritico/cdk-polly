# cdk-polly

##### 
This is an evaluation project to model a complete serverless application in AWS using the [AWS Cloud Development Kit](https://github.com/awslabs/aws-cdk)

The example application is modeled off the example demonstration at [Build A Serverless Text-to-speech application with Amazon Polly](https://aws.amazon.com/blogs/machine-learning/build-your-own-text-to-speech-applications-with-amazon-polly)

###### Prerequisites
* [AWS CLI Configuration](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html)
* [Install AWS CDK](https://awslabs.github.io/aws-cdk/getting-started.html)
 
###### Usage
* cdk synth (--profile=profilename)
* cdk diff (--profile=profilename)
* cdk deploy (--profile=profilename)

###### TODOs
The following items are still left to be done manually after stack creation
* Add CORS method to API Gateway 
* Method Level API Gateway (query param and response)
* Copy API Gateway endpoing into website script
* Website Deployment bug