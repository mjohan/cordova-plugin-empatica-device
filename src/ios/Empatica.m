#import "Empatica.h"

@interface Empatica () <EmpaticaDelegate, EmpaticaDeviceDelegate> {}

@property (nonatomic, weak) EmpaticaDeviceManager *device;

@property (nonatomic, retain) NSString *connectionCallbackId;
@property (nonatomic, retain) NSString *bvpCallbackId;
@property (nonatomic, retain) NSString *ibiCallbackId;
@property (nonatomic, retain) NSString *gsrCallbackId;
@property (nonatomic, retain) NSString *accCallbackId;
@property (nonatomic, retain) NSString *tmpCallbackId;
@property (nonatomic, retain) NSString *batCallbackId;

@property (nonatomic) BOOL isDeviceConnected;
@property (nonatomic) BOOL isDisconnectAttempt;

@end

@implementation Empatica

#pragma mark - Constants
static NSString *const kSensorBVP = @"bvp_sensor";
static NSString *const kSensorIBI = @"ibi_sensor";
static NSString *const kSensorGSR= @"gsr_sensor";
static NSString *const kSensorACC = @"acc_sensor";
static NSString *const kSensorTMP = @"temp_sensor";
static NSString *const kSensorBAT = @"battery_sensor";

#pragma mark - CDVPlugin methods implementation
- (void)initialize:(CDVInvokedUrlCommand*)command {
    NSString *apiKey = [command.arguments objectAtIndex:0];

    [self.commandDelegate runInBackground:^{
        dispatch_async(dispatch_get_main_queue(), ^{
            // we need to call this method from the main thread
            [EmpaticaAPI prepareForResume];
        });

        if (apiKey != nil && [apiKey length] > 0) {
            [EmpaticaAPI authenticateWithAPIKey:apiKey andCompletionHandler:^(BOOL success, NSString *description) {
                CDVPluginResult *pluginResult = nil;

                // send callback depends on the success status
                if (success) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:description];
                } else {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:description];
                }
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }];
        } else {
            // send result error when the string is empty or nil
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Empatica key is invalid"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

- (void)connect:(CDVInvokedUrlCommand*)command {
    self.connectionCallbackId = command.callbackId;
    [EmpaticaAPI discoverDevicesWithDelegate:self]; // it will try to search for several seconds
}

- (void)disconnect:(CDVInvokedUrlCommand*)command {
    if (self.isDeviceConnected) {
        self.connectionCallbackId = command.callbackId;
        self.isDisconnectAttempt = YES;
        [self.device disconnect];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"The device has not been connected"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
    }
}

- (void)subscribe:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;

    if (self.isDeviceConnected) {
        // set the callbackId to the corresponding sensor callbackId
        NSString *sensor = [command.arguments objectAtIndex:0];

        if ([sensor isEqualToString:kSensorBVP]) {
            self.bvpCallbackId = command.callbackId;
        } else if ([sensor isEqualToString:kSensorIBI]) {
            self.ibiCallbackId = command.callbackId;
        } else if ([sensor isEqualToString:kSensorGSR]) {
            self.gsrCallbackId = command.callbackId;
        } else if ([sensor isEqualToString:kSensorACC]) {
            self.accCallbackId = command.callbackId;
        } else if ([sensor isEqualToString:kSensorTMP]) {
            self.tmpCallbackId = command.callbackId;
        } else if ([sensor isEqualToString:kSensorBAT]) {
            self.batCallbackId = command.callbackId;
        }
    } else {
        // send the error callback when the device has not even been connected before
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Please initialize and connect first before start subscribing"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)unsubscribe:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;

    if (self.isDeviceConnected) {
        // set the callbackId to the corresponding sensor callbackId
        NSString *sensor = [command.arguments objectAtIndex:0];

        if ([sensor isEqualToString:kSensorBVP]) {
            self.bvpCallbackId = nil;
        } else if ([sensor isEqualToString:kSensorIBI]) {
            self.ibiCallbackId = nil;
        } else if ([sensor isEqualToString:kSensorGSR]) {
            self.gsrCallbackId = nil;
        } else if ([sensor isEqualToString:kSensorACC]) {
            self.accCallbackId = nil;
        } else if ([sensor isEqualToString:kSensorTMP]) {
            self.tmpCallbackId = nil;
        } else if ([sensor isEqualToString:kSensorBAT]) {
            self.batCallbackId = nil;
        }
    } else {
        // send the error callback when the device has not even been connected before
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Please initialize and connect first before start unsubscribing"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

#pragma mark - EmpaticaDelegate
- (void)didDiscoverDevices:(NSArray *)devices {
    if (devices.count > 0) {
        // Attempt to connect the first discovered device, should be the closest one from phone
        self.device = [devices objectAtIndex:0];
        [self.device connectWithDeviceDelegate:self];
    } else {
        // no device found
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Cannot find any empatica devices"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
    }
}

- (void)didUpdateBLEStatus:(BLEStatus)status {
    if (status == kBLEStatusNotAvailable) {
        // either Bluetooth is turned off or Bluetooth permission is denied
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Bluetooth Low Energy (BLE) is not available"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
    }
}

#pragma mark - EmpaticaDeviceDelegate
- (void)didUpdateDeviceStatus:(DeviceStatus)status forDevice:(EmpaticaDeviceManager *)device {
    if (status == kDeviceStatusDisconnected) {
        [self processDisconnectedEvent];
    } else if (status == kDeviceStatusConnected) {
        [self processConnectedEvent];
    }
}

- (void)didReceiveBVP:(float)bvp withTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device {
    if (self.bvpCallbackId != nil) { // bvpCallbackId is not nil, this means it's subscribed
        NSMutableDictionary *bvpData = [NSMutableDictionary dictionaryWithCapacity:2];
        [bvpData setValue:[NSNumber numberWithFloat:bvp] forKey:@"bvp"];
        [bvpData setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:bvpData];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.bvpCallbackId];
    }
    // else do nothing
}

- (void)didReceiveIBI:(float)ibi withTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device {
    if (self.ibiCallbackId != nil) { // ibiCallbackId is not nil, this means it's subscribed
        NSMutableDictionary *ibiData = [NSMutableDictionary dictionaryWithCapacity:2];
        [ibiData setValue:[NSNumber numberWithFloat:ibi] forKey:@"ibi"];
        [ibiData setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:ibiData];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.ibiCallbackId];
    }
    // else do nothing
}

- (void)didReceiveGSR:(float)gsr withTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device {
    if (self.gsrCallbackId != nil) { // gsrCallbackId is not nil, this means it's subscribed
        NSMutableDictionary *gsrData = [NSMutableDictionary dictionaryWithCapacity:2];
        [gsrData setValue:[NSNumber numberWithFloat:gsr] forKey:@"gsr"];
        [gsrData setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:gsrData];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.gsrCallbackId];
    }
    // else do nothing
}

- (void)didReceiveAccelerationX:(char)x y:(char)y z:(char)z withTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device {
    if (self.accCallbackId != nil) { // accCallbackId is not nil, this means it's subscribed
        NSMutableDictionary *accData = [NSMutableDictionary dictionaryWithCapacity:4];
        [accData setValue:[NSNumber numberWithChar:x] forKey:@"x"];
        [accData setValue:[NSNumber numberWithChar:y] forKey:@"y"];
        [accData setValue:[NSNumber numberWithChar:z] forKey:@"z"];
        [accData setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:accData];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.accCallbackId];
    }
    // else do nothing
}

- (void)didReceiveTemperature:(float)temp withTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device {
    if (self.tmpCallbackId != nil) { // tmpCallbackId is not nil, this means it's subscribed
        NSMutableDictionary *tmpData = [NSMutableDictionary dictionaryWithCapacity:2];
        [tmpData setValue:[NSNumber numberWithFloat:temp] forKey:@"temp"];
        [tmpData setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:tmpData];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.tmpCallbackId];
    }
    // else do nothing
}

- (void)didReceiveBatteryLevel:(float)level withTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device {
    if (self.batCallbackId != nil) { // batCallbackId is not nil, this means it's subscribed
        NSMutableDictionary *batData = [NSMutableDictionary dictionaryWithCapacity:2];
        [batData setValue:[NSNumber numberWithFloat:level] forKey:@"battery"];
        [batData setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:batData];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.batCallbackId];
    }
    // else do nothing
}

- (void)didReceiveTagAtTimestamp:(double)timestamp fromDevice:(EmpaticaDeviceManager *)device{
    // NSLog(@"Received tag with timestamp: %f", timestamp);
}

#pragma mark - Helper methods
- (void)processDisconnectedEvent {
    NSLog(@"processDisconnectedEvent");
    CDVPluginResult *pluginResult = nil;

    if (self.isDisconnectAttempt) {
        // disconnect event occurs after calling disconnect method
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"The device is succesfully disconnected"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
        [EmpaticaAPI prepareForBackground];
        self.isDisconnectAttempt = NO;
    } else if (self.isDeviceConnected) {
        // disconnect event unexpectedly occurs when the device is still streaming data
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"The device is unexpectedly disconnected"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
        [EmpaticaAPI prepareForBackground];
    }
    self.isDeviceConnected = NO;
}

- (void)processConnectedEvent {
    self.isDeviceConnected = YES;
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"The device is succesfully connected"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.connectionCallbackId];
}

@end
