#include "MyCaffeHardwareBridge.h"

#ifdef __APPLE__
#import <Foundation/Foundation.h>
#endif

size_t mycaffe_escpos_build_drawer_kick(uint8_t *out_buf, size_t capacity) {
    if (out_buf == NULL || capacity < 7) {
        return 0;
    }
    /* ESC @ (Initialize) */
    out_buf[0] = 0x1B;
    out_buf[1] = 0x40;
    /* ESC p 0 25 250 (RJ11 Cash Drawer Pin 2: 50ms ON, 250ms OFF) */
    out_buf[2] = 0x1B;
    out_buf[3] = 0x70;
    out_buf[4] = 0x00;
    out_buf[5] = 0x19;
    out_buf[6] = 0xFA;
    return 7;
}

int mycaffe_validate_printer_config(const MyCaffeLanPrinterConfig *cfg) {
    if (cfg == NULL || cfg->host_ipv4 == NULL || cfg->host_ipv4[0] == '\0') {
        return 0;
    }
    if (cfg->tcp_port == 0 || cfg->timeout_ms == 0 || cfg->timeout_ms > 30000) {
        return 0;
    }
    if (cfg->paper_width != MYCAFFE_PAPER_58MM && cfg->paper_width != MYCAFFE_PAPER_80MM) {
        return 0;
    }
    return 1;
}

#ifdef __APPLE__
@interface MyCaffeIosEscPosAdapter : NSObject
- (NSData *)buildCashDrawerPulsePacket;
@end

@implementation MyCaffeIosEscPosAdapter
- (NSData *)buildCashDrawerPulsePacket {
    uint8_t buf[8];
    size_t len = mycaffe_escpos_build_drawer_kick(buf, sizeof(buf));
    return [NSData dataWithBytes:buf length:len];
}
@end
#endif
