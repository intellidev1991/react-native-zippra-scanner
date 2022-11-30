#ifdef RCT_NEW_ARCH_ENABLED
#import "RNZippraScannerSpec.h"

@interface ZippraScanner : NSObject <NativeZippraScannerSpec>
#else
#import <React/RCTBridgeModule.h>

@interface ZippraScanner : NSObject <RCTBridgeModule>
#endif

@end
