package tritico.eval;

import software.amazon.awscdk.App;

public class CdkPollyEval {
    public static void main(final String argv[]) {
        App app = new App();

        new CdkStack(app, "cdk-polly");

        app.run();
    }
}
