export interface WalletCapabilityReport {
  paymentRequestSupported: boolean;
  applePayReady: boolean;
  googlePayReady: boolean;
  secureContext: boolean;
  userAgentSummary: string;
  regulatoryNoteId: string;
  checkedAt: string;
}

declare global {
  interface Window {
    MidtransNew3ds?: {
      version?: string;
      getCardToken?: (...args: unknown[]) => void;
      authenticate?: (...args: unknown[]) => void;
    };
  }
}

export async function detectWalletCapabilities(): Promise<WalletCapabilityReport> {
  const isBrowser = typeof window !== "undefined";
  const secureContext = isBrowser ? Boolean(window.isSecureContext) : false;
  const ua = isBrowser ? navigator.userAgent : "server";
  const hasPaymentRequest = isBrowser && typeof window.PaymentRequest === "function";

  let applePayReady = false;
  let googlePayReady = false;

  if (hasPaymentRequest) {
    try {
      const appleReq = new PaymentRequest(
        [
          {
            supportedMethods: "https://apple.com/apple-pay",
            data: {
              version: 3,
              merchantIdentifier: "com.azuredevid.cassy",
              merchantCapabilities: ["supports3DS"],
              supportedNetworks: ["visa", "masterCard"],
              countryCode: "ID"
            }
          }
        ],
        {
          total: { label: "MyCaffe Capability Check", amount: { currency: "IDR", value: "10000" } }
        }
      );
      applePayReady = Boolean(await appleReq.canMakePayment());
    } catch {
      applePayReady = false;
    }

    try {
      const googleReq = new PaymentRequest(
        [
          {
            supportedMethods: "https://google.com/pay",
            data: {
              environment: "TEST",
              apiVersion: 2,
              apiVersionMinor: 0,
              merchantInfo: {
                merchantName: "MyCaffe by AzureDevID"
              },
              allowedPaymentMethods: [
                {
                  type: "CARD",
                  parameters: {
                    allowedAuthMethods: ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                    allowedCardNetworks: ["VISA", "MASTERCARD"]
                  }
                }
              ]
            }
          }
        ],
        {
          total: { label: "MyCaffe Capability Check", amount: { currency: "IDR", value: "10000" } }
        }
      );
      googlePayReady = Boolean(await googleReq.canMakePayment());
    } catch {
      googlePayReady = false;
    }
  }

  return {
    paymentRequestSupported: hasPaymentRequest,
    applePayReady,
    googlePayReady,
    secureContext,
    userAgentSummary: ua.slice(0, 96),
    regulatoryNoteId:
      "QRIS Bank Indonesia merupakan jalur pembayaran domestik utama. Apple Pay dan Google Pay beroperasi melalui tokenisasi kartu EMV 3DS Midtrans Core API untuk kartu penerbit yang mendukung transaksi IDR.",
    checkedAt: new Date().toISOString()
  };
}

export function loadMidtransCore3dsSdk(
  clientKey: string,
  environment: "sandbox" | "production"
): Promise<{ loaded: boolean; scriptUrl: string; hasGlobalObject: boolean }> {
  const scriptUrl =
    environment === "production"
      ? "https://api.midtrans.com/v2/assets/js/midtrans-new-3ds.min.js"
      : "https://api.sandbox.midtrans.com/v2/assets/js/midtrans-new-3ds.min.js";

  return new Promise((resolve, reject) => {
    if (typeof document === "undefined") {
      reject(new Error("SDK Midtrans hanya dapat dimuat di peramban."));
      return;
    }

    const existing = document.getElementById("midtrans-script") as HTMLScriptElement | null;
    if (existing) {
      existing.remove();
    }

    const script = document.createElement("script");
    script.id = "midtrans-script";
    script.type = "text/javascript";
    script.src = scriptUrl;
    script.async = true;
    script.setAttribute("data-environment", environment);
    script.setAttribute("data-client-key", clientKey.trim());

    script.onload = () => {
      resolve({
        loaded: true,
        scriptUrl,
        hasGlobalObject: Boolean(window.MidtransNew3ds)
      });
    };
    script.onerror = () => {
      reject(new Error(`Gagal memuat skrip resmi Midtrans dari ${scriptUrl}. Periksa koneksi internet Anda.`));
    };

    document.head.appendChild(script);
  });
}

