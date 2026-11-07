#ifndef MYCAFFE_HARDWARE_BRIDGE_H
#define MYCAFFE_HARDWARE_BRIDGE_H

/*
 * MyCaffe by AzureDevID - iOS & iPadOS Native Hardware Bridge
 * Official Bundle Identifier: online.abbdullaziz.mycaffe
 * Protocol: ESC/POS over LAN TCP:9100 & RJ11 Cash Drawer Kick
 */

#include <stddef.h>
#include <stdint.h>

#define MYCAFFE_IOS_BUNDLE_ID "online.abbdullaziz.mycaffe"

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
 * Returns the canonical Apple bundle identifier ("online.abbdullaziz.mycaffe").
 */
const char *mycaffe_ios_bundle_identifier(void);

/**
 * Builds an ESC/POS initialization + RJ11 Pin-2 Cash Drawer pulse packet into out_buf.
 * Returns number of bytes written, or 0 if capacity < 7.
 */
size_t mycaffe_escpos_build_drawer_kick(uint8_t *out_buf, size_t capacity);

/**
 * Validates IPv4 LAN port and paper width parameters before opening BSD/NWConnection socket on iOS.
 * Returns 1 when valid, 0 when invalid.
 */
int mycaffe_validate_printer_config(const MyCaffeLanPrinterConfig *cfg);

#endif /* MYCAFFE_HARDWARE_BRIDGE_H */
