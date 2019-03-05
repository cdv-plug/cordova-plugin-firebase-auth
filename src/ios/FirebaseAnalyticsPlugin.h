#import <Cordova/CDV.h>

@interface FirebaseAuthPlugin : CDVPlugin

- (void)logEvent:(CDVInvokedUrlCommand*)command;
- (void)setUserId:(CDVInvokedUrlCommand*)command;
- (void)setCurrentScreen:(CDVInvokedUrlCommand*)command;

@end
