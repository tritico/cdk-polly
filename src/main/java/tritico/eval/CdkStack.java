package tritico.eval;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SnsEventSource;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CdkStack extends Stack {

    public CdkStack(final App parent, final String name) {
        this(parent, name, null);
    }

    public CdkStack(final App parent, final String name, final StackProps props) {
        super(parent, name, props);

        // S3 Bucket
        Bucket bucket = new Bucket(this, "audioposts-504", BucketProps.builder()
                .withBucketName("audioposts-504")
                .withVersioned(false)
                .build());

        // Dynamo Table
        Attribute key = Attribute.builder().withName("id").withType(AttributeType.String).build();
        Table table = new Table(this, "posts", TableProps.builder()
                .withPartitionKey(key)
                .withTableName("posts")
                .build());

        // SNS Topic
        Topic topic = new Topic(this, "new_posts", TopicProps.builder()
                .withDisplayName("New Posts")
                .withTopicName("new_posts")
                .build());

        // IAM Role and Policy
        Role role = new Role(this, "polly_app_role", RoleProps.builder()
                .withRoleName("polly_app_role")
                .withAssumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build());

        role.addToPolicy(new PolicyStatement()
                .addAllResources()
                .addAction("polly:SynthesizeSpeech")
                .addAction("dynamodb:Query")
                .addAction("dynamodb:Scan")
                .addAction("dynamodb:PutItem")
                .addAction("dynamodb:UpdateItem")
                .addAction("sns:Publish")
                .addAction("s3:PutObject")
                .addAction("s3:PutObjectAcl")
                .addAction("s3:GetBucketLocation")
                .addAction("logs:CreateLogGroup")
                .addAction("logs:CreateLogStream")
                .addAction("logs:PutLogEvents")
        );

        // Lambdas
        String lambda_path = getClass().getResource("/lamdas").getPath();
        Function lambdaNewPost = AddLambdaNewPost(lambda_path, table, topic);
        Function lambdaGetPost = AddLambdaGetPost(lambda_path, table);
        AddLambdaSynthesize(lambda_path, table, topic, bucket, role);


        // API Gateway
        List<EndpointType> endpointTypes = new ArrayList<>();
        endpointTypes.add(EndpointType.Regional);

        RestApi api = new RestApi(this, "PostReaderAPI1", RestApiProps.builder()
                .withDescription("Api with Lambda integration to Get/Post messages")
                .withEndpointTypes(endpointTypes)
                .build());

        // Method Shared
        HashMap<String, String> responseTemplates = new HashMap<String, String>();
        responseTemplates.put("application/json", "");
        IntegrationResponse r = IntegrationResponse.builder()
                .withStatusCode("200")
                .withResponseTemplates(responseTemplates).build();

        ArrayList<IntegrationResponse> responses = new ArrayList<>();
        responses.add(r);

        // GET Method
        HashMap<String, String> requestTemplates = new HashMap<String, String>();
        requestTemplates.put("application/json", "{\"postId\" : \"$input.params('postId')\"}");
        Method methGet = api.getRoot().addMethod("GET",
                new LambdaIntegration(lambdaGetPost, LambdaIntegrationOptions.builder()
                        .withProxy(false)
                        .withRequestTemplates(requestTemplates)
                        .withIntegrationResponses(responses)
                        .build()));


        // POST Method
        Method methPost =  api.getRoot().addMethod("POST",
                new LambdaIntegration(lambdaNewPost, LambdaIntegrationOptions.builder()
                        .withProxy(false)
                        .withIntegrationResponses(responses)
                        .build()));

        //AddOptionsCors(api);

        // Website Bucket
        Bucket bucketWeb = new Bucket(this, "audioposts-web", BucketProps.builder()
                .withBucketName("audioposts-web")
                .withWebsiteIndexDocument("index.html")
                .withPublicReadAccess(true)
                .withVersioned(false)
                .build());

//        // Website Deployment
//        String web_path = getClass().getResource("/web").getPath();
//        new BucketDeployment(this, "audioposts-web-deployment", BucketDeploymentProps.builder()
//                .withSource(Source.asset(web_path))
//                .withDestinationBucket(bucketWeb)
//                .withDestinationKeyPrefix("test")
//                .build());
    }

    private Function AddLambdaNewPost(String lambda_path, Table table, Topic topic) {
        // Set Environment Variables
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("SNS_TOPIC", topic.getTopicArn());
        map.put("DB_TABLE_NAME", table.getTableName());

        // Create Lambda Function
        Function fun = new Function(this, "PostReader_NewPost", FunctionProps.builder()
                .withFunctionName("PostReader_NewPost")
                .withRuntime(Runtime.PYTHON27)
                .withCode(Code.asset(lambda_path))
                .withHandler("new_post.lambda_handler")
                .withEnvironment(map)
                .build());

        // Grant Permissions
        table.grantWriteData(fun.getRole());
        topic.grantPublish(fun.getRole());

        return fun;
    }

    private void AddLambdaSynthesize(String lambda_path, Table table, Topic topic, Bucket bucket, Role role) {
        // Set Environment Variables
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("DB_TABLE_NAME", table.getTableName());
        map.put("BUCKET_NAME", bucket.getBucketName());

        // Create Lambda Function
        Function fun = new Function(this, "PostReader_ConvertToAudio", FunctionProps.builder()
                .withFunctionName("PostReader_ConvertToAudio")
                .withRuntime(Runtime.PYTHON27)
                .withCode(Code.asset(lambda_path))
                .withHandler("synthesize.lambda_handler")
                .withEnvironment(map)
                .withTimeout(300)
                .withRole(role)
                .build());

        // Add Trigger
        SnsEventSource trigger = new SnsEventSource(topic);
        fun.addEventSource(trigger);

        // Grant Permissions
        table.grantReadData(fun.getRole());
        table.grantWriteData(fun.getRole());
        bucket.grantPut(fun.getRole());
    }

    private Function AddLambdaGetPost(String lambda_path, Table table) {
        // Set Environment Variables
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("DB_TABLE_NAME", table.getTableName());

        // Create Lambda Function
        Function fun = new Function(this, "PostReader_GetPost", FunctionProps.builder()
                .withFunctionName("PostReader_GetPost")
                .withRuntime(Runtime.PYTHON27)
                .withCode(Code.asset(lambda_path))
                .withHandler("get_post.lambda_handler")
                .withEnvironment(map)
                .build());

        // Grant Permissions
        table.grantReadData(fun.getRole());

        return fun;
    }


}