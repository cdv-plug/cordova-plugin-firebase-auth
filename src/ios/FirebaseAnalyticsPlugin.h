#import <Cordova/CDV.h>
@import Firebase;

@interface FirebaseAnalyticsPlugin : CDVPlugin

- (void)logEvent:(CDVInvokedUrlCommand*)command;
- (void)setUserId:(CDVInvokedUrlCommand*)command;
- (void)setUserProperty:(CDVInvokedUrlCommand*)command;
- (void)setCurrentScreen:(CDVInvokedUrlCommand*)command;
- (void)getValue:(CDVInvokedUrlCommand*)command;

@property (nonatomic, strong) FIRRemoteConfig *remoteConfig;

@end
