#import <Cordova/CDVPlugin.h>
#import <EmpaLink-ios-0.7-full/EmpaticaAPI-0.7.h>

@interface Empatica : CDVPlugin <EmpaticaDelegate, EmpaticaDeviceDelegate>

/*!
    @brief Initializes EmpaticaAPI with provided api key, then callback based on the result
 */
- (void)initialize:(CDVInvokedUrlCommand*)command;

/*!
    @brief Connects to the device then waits for didUpdateDeviceStatus callback
 */
- (void)connect:(CDVInvokedUrlCommand*)command;

/*!
    @brief Disconnects to the device then waits for didUpdateDeviceStatus callback
 */
- (void)disconnect:(CDVInvokedUrlCommand*)command;

/*!
    @brief Subscribes to one of sensors provided by empatica device
 */
- (void)subscribe:(CDVInvokedUrlCommand*)command;

/*!
    @brief Unsubscribe to one of sensors provided by empatica device
 */
- (void)unsubscribe:(CDVInvokedUrlCommand*)command;

@end
