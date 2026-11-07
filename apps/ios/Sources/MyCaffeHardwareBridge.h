#ifndef MYCAFFE_HARDWARE_BRIDGE_H
#define MYCAFFE_HARDWARE_BRIDGE_H

#include <stddef.h>
#include <stdint.h>

typedef enum {
    MYCAFFE_PAPER_58MM = 58,
    MYCAFFE_PAPER_80MM = 80
} MyCaffePaperWidth;

typedef struct {
    const char *host_ipv4;
    uint16_t tcp_port;
    MyCaffePaperWidth paper_width;
    uint32_t timeout_ms;
} MyCaffeLanPrinterConfig;

/**
 * Builds an ESC/POS initialization + RJ11 Pin-2 Cash Drawer pulse packet into out_buf.
 * Returns number of bytes written, or 0 if capacity < 7.
 */
size_t mycaffe_escpos_build_drawer_kick(uint8_t *out_buf, size_t capacity);

/**
 * Validates IPv4 LAN port & paper width parameters before opening BSD/NWConnection socket on iOS.
 * Returns 1 when valid, 0 when invalid.
 */
int mycaffe_validate_printer_config(const MyCaffeLanPrinterConfig *cfg);

#endif /* MYCAFFE_HARDWARE_BRIDGE_H */
