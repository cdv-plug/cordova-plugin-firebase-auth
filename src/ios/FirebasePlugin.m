#import "FirebasePlugin.h"
@import Firebase;
@import GoogleSignIn;

@implementation FirebasePlugin
- (void)pluginInitialize {
    @autoreleasepool {
        [FIRApp configure];
        [FIRAuth auth];

        [self configureGoogleSignin];
    }
}

- (void)configureGoogleSignin {
    GIDSignIn* signIn = [GIDSignIn sharedInstance];
    signIn.delegate = self;
    signIn.uiDelegate = self;
    signIn.clientID = [[[FIRApp defaultApp] options] clientID];
    signIn.shouldFetchBasicProfile = YES;

    self.googleSigninInstance = signIn;
}

- (void)logout:(CDVInvokedUrlCommand *)command {
    NSLog(@"Google logout");
    [self.googleSigninInstance signOut];

    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"ok"] callbackId:command.callbackId];
}

- (void)googleLogin:(CDVInvokedUrlCommand *)command {
    NSLog(@"Google sign in");
    [self.googleSigninInstance signIn];
    self.currentSigninCallbackId = command.callbackId;
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
  sourceApplication:(NSString *)sourceApplication
         annotation:(id)annotation {
    return [[GIDSignIn sharedInstance] handleURL:url
                               sourceApplication:sourceApplication
                                      annotation:annotation];
}

- (void) callbackError:(NSString *) message withCallbackId:(NSString *) callbackId {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message] callbackId:callbackId];
}

- (void) callbackDictionary:(NSDictionary *) dictionary withCallbackId:(NSString *) callbackId {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary] callbackId:self.currentSigninCallbackId];
}

- (void)signIn:(GIDSignIn *)signIn didSignInForUser:(GIDGoogleUser *)user withError:(NSError *)error {
    NSLog(@"Signed in user");
    if (error != nil) {
        return [self callbackError:@"Error on GID Signin view" withCallbackId:self.currentSigninCallbackId];
    } else {
        GIDAuthentication *authentication = user.authentication;
        FIRAuthCredential *credential =
        [FIRGoogleAuthProvider credentialWithIDToken:authentication.idToken
                                         accessToken:authentication.accessToken];
        NSString * serverAuthCode = user.serverAuthCode ? user.serverAuthCode : @"";
        NSString * idToken = user.authentication.idToken;

        [[FIRAuth auth] signInAndRetrieveDataWithCredential:credential
             completion:^(FIRAuthDataResult * _Nullable authResult,
                          NSError * _Nullable error) {
                 if (error) {
                     return [self callbackError:@"GIDAuth success, but firebase failed" withCallbackId:self.currentSigninCallbackId];
                 }
                 // User successfully signed in. Get user data from the FIRUser object
                 if (authResult == nil) {
                     return [self callbackError:@"GID Auth success, but firebase is null" withCallbackId:self.currentSigninCallbackId];
                 }
                 FIRUser *user = authResult.user;

                 [self callbackDictionary:@{
                                            @"uid": user.uid,
                                            @"displayName": user.displayName ? user.displayName : @"",
                                            @"email": user.email ? user.email : @"",
                                            @"phoneNumber": user.phoneNumber ? user.phoneNumber : @"",
                                            @"photoURL": user.photoURL ? user.photoURL.absoluteString : @"",
                                            @"providerId": user.providerID ? user.providerID : @"",

                                            @"serverAuthCode": serverAuthCode,
                                            @"idToken": idToken,
                                            }
                       withCallbackId:self.currentSigninCallbackId];
             }];



    }
}

- (void)signIn:(GIDSignIn *)signIn didDisconnectWithUser:(GIDGoogleUser *)user withError:(NSError *)error {
    // Perform any operations when the user disconnects from app here.
    NSLog(@"Disconnected user");
}

- (void)signIn:(GIDSignIn *)signIn presentViewController:(UIViewController *)viewController
{
    NSLog(@"signin view");
    [self.viewController presentViewController:viewController animated:YES completion:nil];
}

- (void)signIn:(GIDSignIn *)signIn dismissViewController:(UIViewController *)viewController
{
    NSLog(@"dismiss view");
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
}

@end
