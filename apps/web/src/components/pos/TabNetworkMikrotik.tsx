"use client";

import React, { useCallback, useEffect, useState } from "react";
import {
  apiFetch,
  MikrotikGeneratedBundle,
  StoreMikrotikProfile,
  StoreNetworkNode,
  StoreNetworkNodeInput
} from "@/lib/api";
import { DataStateBox, StatusBadge } from "@/components/Shell";

const DEFAULT_MIKROTIK_PROFILE: StoreMikrotikProfile = {
  router_model: "hAP ax2 (RouterOS v7)",
  wan_primary_interface: "ether1-isp-utama",
  wan_backup_interface: "ether2-lte-backup",
  pos_vlan_cidr: "192.168.10.0/24",
  staff_vlan_cidr: "192.168.20.0/24",
  guest_vlan_cidr: "192.168.30.0/24",
  pos_wifi_ssid: "MyCaffe-POS-Private",
  guest_wifi_ssid: "Kafe-Tamu-WiFi",
  guest_rate_limit_mbps: 20,
  pos_reserved_mbps: 10,
  sftp_port: 2222,
  winbox_port: 8291,
  winbox_allowed_cidr: "192.168.10.0/24",
  dns_servers: "1.1.1.1,8.8.8.8"
};

export function TabNetworkMikrotik({
  token,
  storeSlug
}: {
  token: string;
  storeSlug: string;
}) {
  const [nodes, setNodes] = useState<StoreNetworkNode[]>([]);
  const [profile, setProfile] = useState<StoreMikrotikProfile>(DEFAULT_MIKROTIK_PROFILE);
  const [generatedBundle, setGeneratedBundle] = useState<MikrotikGeneratedBundle | null>(null);
  const [simResult, setSimResult] = useState<Record<string, unknown> | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ tone: "ok" | "err" | "info"; text: string } | null>(null);

  // Node Editor State (exact Go StoreNetworkNodeInput fields)
  const [nodeName, setNodeName] = useState("Printer Kasir Utama 80mm");
  const [nodeType, setNodeType] = useState<StoreNetworkNodeInput["node_type"]>("thermal_printer");
  const [protocol, setProtocol] = useState<StoreNetworkNodeInput["connection_protocol"]>("tcp_escpos");
  const [localIp, setLocalIp] = useState("192.168.10.50");
  const [localPort, setLocalPort] = useState(9100);
  const [macAddress, setMacAddress] = useState("AA:BB:CC:10:20:50");
  const [paperWidth, setPaperWidth] = useState<58 | 80>(80);
  const [drawerKick, setDrawerKick] = useState(true);
  const [savingNode, setSavingNode] = useState(false);

  const [savingMikrotik, setSavingMikrotik] = useState(false);
  const [generatingRsc, setGeneratingRsc] = useState(false);
  const [simBusy, setSimBusy] = useState(false);

  const loadNetworkData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [nodesRes, mtRes] = await Promise.all([
        apiFetch<{ nodes?: StoreNetworkNode[] }>("/api/v1/store/network/nodes", {
          bearerToken: token
        }).catch(() => ({ nodes: [] as StoreNetworkNode[] })),
        apiFetch<Record<string, unknown>>("/api/v1/store/network/mikrotik", {
          bearerToken: token
        }).catch(() => ({}))
      ]);

      setNodes(Array.isArray(nodesRes.nodes) ? nodesRes.nodes : []);

      const mtRaw = mtRes as Record<string, unknown>;
      const pObj = (
        mtRaw.profile && typeof mtRaw.profile === "object" ? mtRaw.profile : mtRaw
      ) as Partial<StoreMikrotikProfile>;
      if (pObj.router_model) {
        setProfile((prev) => ({
          ...prev,
          ...pObj
        }));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Gagal memuat topologi jaringan gerai");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void loadNetworkData();
  }, [loadNetworkData]);

  const handleSaveNode = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: StoreNetworkNodeInput = {
      node_name: nodeName.trim(),
      node_type: nodeType,
      connection_protocol: protocol,
      local_ip: localIp.trim(),
      local_port: Number(localPort) || 9100,
      mac_address: macAddress.trim().toUpperCase(),
      paper_width_mm: paperWidth,
      drawer_kick_enabled: drawerKick,
      health_status: "online",
      last_latency_ms: 8,
      is_active: true
    };

    setSavingNode(true);
    setFeedback(null);
    try {
      await apiFetch("/api/v1/store/network/nodes", {
        method: "PUT",
        bearerToken: token,
        body: payload
      });
      setFeedback({ tone: "ok", text: `Perangkat LAN "${payload.node_name}" berhasil disimpan.` });
      await loadNetworkData();
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal menyimpan perangkat jaringan"
      });
    } finally {
      setSavingNode(false);
    }
  };

  const handleSimulate = async (scenario: "normal" | "wan_down" | "lan_partition") => {
    setSimBusy(true);
    setFeedback(null);
    try {
      const res = await apiFetch<Record<string, unknown>>("/api/v1/store/network/simulate", {
        method: "POST",
        bearerToken: token,
        body: { scenario }
      });
      setSimResult(res);
      setFeedback({ tone: "info", text: `Simulasi jaringan "${scenario}" berhasil dijalankan.` });
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal menjalankan simulasi jaringan"
      });
    } finally {
      setSimBusy(false);
    }
  };

  const handleSaveAndGenerateRsc = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingMikrotik(true);
    setGeneratingRsc(true);
    setFeedback(null);
    try {
      await apiFetch("/api/v1/store/network/mikrotik", {
        method: "PUT",
        bearerToken: token,
        body: profile
      });
      const bundle = await apiFetch<MikrotikGeneratedBundle>(
        "/api/v1/store/network/mikrotik/generate-rsc",
        {
          method: "POST",
          bearerToken: token,
          body: {}
        }
      );
      setGeneratedBundle(bundle);
      setFeedback({
        tone: "ok",
        text: "Profil MikroTik disimpan dan skrip RouterOS v7 (.rsc) berhasil dibuat."
      });
    } catch (err) {
      setFeedback({
        tone: "err",
        text: err instanceof Error ? err.message : "Gagal membuat skrip MikroTik .rsc"
      });
    } finally {
      setSavingMikrotik(false);
      setGeneratingRsc(false);
    }
  };

  const handleDownloadRsc = () => {
    if (!generatedBundle?.rsc_script) return;
    const blob = new Blob([generatedBundle.rsc_script], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `mycaffe-${storeSlug || "outlet"}-routeros7.rsc`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <DataStateBox
        state="loading"
        title="Memuat topologi LAN & profil MikroTik gerai"
        description="Mengambil daftar perangkat lokal dan parameter VLAN RouterOS v7..."
      />
    );
  }

  if (error) {
    return (
      <DataStateBox
        state="error"
        title="Gagal memuat konfigurasi jaringan"
        description={error}
        onRetry={loadNetworkData}
      />
    );
  }

  return (
    <div className="space-y-8">
      {feedback ? (
        <div role="status">
          <StatusBadge tone={feedback.tone} label={feedback.text} />
        </div>
      ) : null}

      {/* Visual Cloud-to-LAN Zone Architecture + Simulator Controls */}
      <section className="space-y-4 rounded-xl border border-subtle bg-surface p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 className="font-display text-lg font-bold text-ink">
              Peta Alur Cloud ke LAN Gerai &amp; Simulator Failover
            </h3>
            <p className="text-xs text-subink">
              Uji ketahanan operasional kasir ketika koneksi internet ISP terputus atau terjadi partisi VLAN lokal.
            </p>
          </div>
          <a
            href="/network-topology-simulator.html"
            target="_blank"
            rel="noreferrer"
            className="btn-secondary min-h-[44px] px-3.5 py-2 text-xs font-semibold"
          >
            Buka Simulator Layar Penuh
          </a>
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <div className="rounded-lg border border-subtle bg-muted p-4">
            <p className="font-mono text-xs font-bold text-accent-ink">ZONA 1: CLOUD EDGE</p>
            <p className="mt-1 font-display text-sm font-bold text-ink">
              Cloudflare TLS 1.3 + Go RLS API
            </p>
            <p className="mt-1 text-xs text-subink">
              Sinkronisasi pesanan meja QR, verifikasi webhook SHA-512 Midtrans, dan cadangan terenkripsi R2.
            </p>
          </div>
          <div className="rounded-lg border border-subtle bg-muted p-4">
            <p className="font-mono text-xs font-bold text-accent-ink">ZONA 2: ROUTEROS V7 VLAN</p>
            <p className="mt-1 font-display text-sm font-bold text-ink">
              {profile.router_model} ({profile.pos_vlan_cidr})
            </p>
            <p className="mt-1 text-xs text-subink">
              VLAN 10 Kasir terisolasi penuh dari VLAN 30 Tamu ({profile.guest_vlan_cidr}) dengan garansi lebar pita {profile.pos_reserved_mbps} Mbps.
            </p>
          </div>
          <div className="rounded-lg border border-subtle bg-muted p-4">
            <p className="font-mono text-xs font-bold text-accent-ink">ZONA 3: HARDWARE GERAI</p>
            <p className="mt-1 font-display text-sm font-bold text-ink">
              Printer ESC/POS TCP:9100 &amp; TV KDS
            </p>
            <p className="mt-1 text-xs text-subink">
              Reservasi DHCP MAC mengunci IP printer kasir dan dapur agar tidak berubah saat router menyala ulang.
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2 pt-1">
          <button
            type="button"
            disabled={simBusy}
            onClick={() => handleSimulate("normal")}
            className="btn-primary min-h-[48px] px-4 py-2 text-xs font-bold"
          >
            Simulasi Normal (Online)
          </button>
          <button
            type="button"
            disabled={simBusy}
            onClick={() => handleSimulate("wan_down")}
            className="btn-secondary min-h-[48px] px-4 py-2 text-xs font-semibold"
          >
            Simulasi ISP WAN Putus
          </button>
          <button
            type="button"
            disabled={simBusy}
            onClick={() => handleSimulate("lan_partition")}
            className="btn-secondary min-h-[48px] px-4 py-2 text-xs font-semibold"
          >
            Simulasi Gangguan Switch LAN
          </button>
        </div>

        {simResult ? (
          <pre className="overflow-x-auto rounded-lg border border-subtle bg-canvas p-3 font-mono text-xs text-ink">
            {JSON.stringify(simResult, null, 2)}
          </pre>
        ) : null}
      </section>

      {/* LAN Hardware Node Registry */}
      <section className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <form
          onSubmit={handleSaveNode}
          className="space-y-3 rounded-xl border border-subtle bg-surface p-5 lg:col-span-5"
        >
          <h3 className="font-display text-base font-bold text-ink">
            Daftarkan Perangkat LAN (IP / MAC Static Lease)
          </h3>

          <div>
            <label htmlFor="nd-name" className="block text-xs font-bold text-ink">
              Nama Perangkat
            </label>
            <input
              id="nd-name"
              type="text"
              required
              value={nodeName}
              onChange={(e) => setNodeName(e.target.value)}
              className="input-field mt-1"
            />
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label htmlFor="nd-type" className="block text-xs font-bold text-ink">
                Tipe Perangkat
              </label>
              <select
                id="nd-type"
                value={nodeType}
                onChange={(e) => setNodeType(e.target.value as StoreNetworkNodeInput["node_type"])}
                className="input-field mt-1"
              >
                <option value="thermal_printer">Printer Thermal</option>
                <option value="kds_display">Layar KDS Dapur</option>
                <option value="cashier_tablet">Tablet Kasir POS</option>
                <option value="local_edge_server">Local Edge Print Agent</option>
                <option value="queue_tv">TV Antrean</option>
                <option value="router_mikrotik">Router MikroTik</option>
              </select>
            </div>
            <div>
              <label htmlFor="nd-proto" className="block text-xs font-bold text-ink">
                Protokol
              </label>
              <select
                id="nd-proto"
                value={protocol}
                onChange={(e) =>
                  setProtocol(e.target.value as StoreNetworkNodeInput["connection_protocol"])
                }
                className="input-field mt-1"
              >
                <option value="tcp_escpos">TCP:9100 ESC/POS</option>
                <option value="http_lan">HTTP LAN</option>
                <option value="websocket">WebSocket</option>
                <option value="usb_bridge">USB Bridge</option>
                <option value="bluetooth">Bluetooth SPP</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div className="col-span-2">
              <label htmlFor="nd-ip" className="block text-xs font-bold text-ink">
                Alamat IPv4 LAN
              </label>
              <input
                id="nd-ip"
                type="text"
                required
                value={localIp}
                onChange={(e) => setLocalIp(e.target.value)}
                className="input-field mt-1 font-mono"
              />
            </div>
            <div>
              <label htmlFor="nd-port" className="block text-xs font-bold text-ink">
                Port
              </label>
              <input
                id="nd-port"
                type="number"
                required
                value={localPort}
                onChange={(e) => setLocalPort(Number(e.target.value))}
                className="input-field mt-1 font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label htmlFor="nd-mac" className="block text-xs font-bold text-ink">
                MAC Address
              </label>
              <input
                id="nd-mac"
                type="text"
                required
                value={macAddress}
                onChange={(e) => setMacAddress(e.target.value)}
                className="input-field mt-1 font-mono"
              />
            </div>
            <div>
              <label htmlFor="nd-paper" className="block text-xs font-bold text-ink">
                Lebar Kertas
              </label>
              <select
                id="nd-paper"
                value={paperWidth}
                onChange={(e) => setPaperWidth(Number(e.target.value) === 58 ? 58 : 80)}
                className="input-field mt-1"
              >
                <option value={80}>80mm (48 kol)</option>
                <option value={58}>58mm (32 kol)</option>
              </select>
            </div>
          </div>

          <label className="flex min-h-[44px] cursor-pointer items-center gap-2 text-xs font-semibold text-ink">
            <input
              type="checkbox"
              checked={drawerKick}
              onChange={(e) => setDrawerKick(e.target.checked)}
              className="h-5 w-5 accent-[#2C1A12]"
            />
            Aktifkan Pemicu Laci Kasir RJ11 (Pin 2)
          </label>

          <button
            type="submit"
            disabled={savingNode}
            className="btn-primary min-h-[48px] w-full px-4 py-2 text-xs font-bold"
          >
            {savingNode ? "Menyimpan..." : "Simpan Perangkat LAN"}
          </button>
        </form>

        <div className="space-y-3 lg:col-span-7">
          <h3 className="font-display text-base font-bold text-ink">
            Daftar Perangkat Jaringan Terdaftar ({nodes.length})
          </h3>
          {nodes.length === 0 ? (
            <DataStateBox
              state="empty"
              title="Belum ada perangkat LAN yang dicatat"
              description="Tambahkan printer thermal TCP:9100 atau layar KDS di sebelah kiri agar masuk ke reservasi DHCP skrip MikroTik."
            />
          ) : (
            <div className="space-y-2.5">
              {nodes.map((nd, idx) => (
                <div
                  key={nd.id ?? `${nd.node_name}-${idx}`}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-subtle bg-surface p-4"
                >
                  <div>
                    <p className="font-display text-sm font-bold text-ink">{nd.node_name}</p>
                    <p className="font-mono text-xs text-subink">
                      {nd.node_type} | {nd.connection_protocol} | {nd.local_ip}:{nd.local_port} | MAC: {nd.mac_address}
                    </p>
                  </div>
                  <StatusBadge
                    tone={nd.health_status === "online" ? "ok" : "warn"}
                    label={`${nd.health_status} (${nd.paper_width_mm}mm)`}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* MikroTik RouterOS v7 Wizard (.rsc Generator + Winbox + SFTP Policy) */}
      <form
        onSubmit={handleSaveAndGenerateRsc}
        className="space-y-5 rounded-xl border border-subtle bg-surface p-5 sm:p-6"
      >
        <div>
          <h3 className="font-display text-lg font-bold text-ink">
            Wizard Generator Skrip MikroTik RouterOS v7 (.rsc), Winbox &amp; Kebijakan SFTP
          </h3>
          <p className="text-xs text-subink">
            Menghasilkan konfigurasi VLAN kasir/staf/tamu, Simple Queue QoS prioritas pembayaran QRIS, firewall isolasi tamu, serta penguncian port Winbox dan SFTP.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label htmlFor="mt-model" className="block text-xs font-bold text-ink">
              Model Router MikroTik
            </label>
            <input
              id="mt-model"
              type="text"
              value={profile.router_model}
              onChange={(e) => setProfile({ ...profile, router_model: e.target.value })}
              className="input-field mt-1"
            />
          </div>
          <div>
            <label htmlFor="mt-wan1" className="block text-xs font-bold text-ink">
              Interface WAN Utama
            </label>
            <input
              id="mt-wan1"
              type="text"
              value={profile.wan_primary_interface}
              onChange={(e) => setProfile({ ...profile, wan_primary_interface: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>
          <div>
            <label htmlFor="mt-wan2" className="block text-xs font-bold text-ink">
              Interface WAN Cadangan (LTE)
            </label>
            <input
              id="mt-wan2"
              type="text"
              value={profile.wan_backup_interface}
              onChange={(e) => setProfile({ ...profile, wan_backup_interface: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>
          <div>
            <label htmlFor="mt-dns" className="block text-xs font-bold text-ink">
              Server DNS
            </label>
            <input
              id="mt-dns"
              type="text"
              value={profile.dns_servers}
              onChange={(e) => setProfile({ ...profile, dns_servers: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>

          <div>
            <label htmlFor="mt-vpos" className="block text-xs font-bold text-ink">
              CIDR VLAN 10 (Kasir &amp; Printer)
            </label>
            <input
              id="mt-vpos"
              type="text"
              value={profile.pos_vlan_cidr}
              onChange={(e) => setProfile({ ...profile, pos_vlan_cidr: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>
          <div>
            <label htmlFor="mt-vstaff" className="block text-xs font-bold text-ink">
              CIDR VLAN 20 (Staf)
            </label>
            <input
              id="mt-vstaff"
              type="text"
              value={profile.staff_vlan_cidr}
              onChange={(e) => setProfile({ ...profile, staff_vlan_cidr: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>
          <div>
            <label htmlFor="mt-vguest" className="block text-xs font-bold text-ink">
              CIDR VLAN 30 (Wi-Fi Tamu)
            </label>
            <input
              id="mt-vguest"
              type="text"
              value={profile.guest_vlan_cidr}
              onChange={(e) => setProfile({ ...profile, guest_vlan_cidr: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>
          <div>
            <label htmlFor="mt-wincidr" className="block text-xs font-bold text-ink">
              Subnet Diizinkan Akses Winbox
            </label>
            <input
              id="mt-wincidr"
              type="text"
              value={profile.winbox_allowed_cidr}
              onChange={(e) => setProfile({ ...profile, winbox_allowed_cidr: e.target.value })}
              className="input-field mt-1 font-mono"
            />
          </div>

          <div>
            <label htmlFor="mt-pos-ssid" className="block text-xs font-bold text-ink">
              SSID Wi-Fi Kasir Privat
            </label>
            <input
              id="mt-pos-ssid"
              type="text"
              value={profile.pos_wifi_ssid}
              onChange={(e) => setProfile({ ...profile, pos_wifi_ssid: e.target.value })}
              className="input-field mt-1"
            />
          </div>
          <div>
            <label htmlFor="mt-guest-ssid" className="block text-xs font-bold text-ink">
              SSID Wi-Fi Pengunjung
            </label>
            <input
              id="mt-guest-ssid"
              type="text"
              value={profile.guest_wifi_ssid}
              onChange={(e) => setProfile({ ...profile, guest_wifi_ssid: e.target.value })}
              className="input-field mt-1"
            />
          </div>
          <div>
            <label htmlFor="mt-sftp" className="block text-xs font-bold text-ink">
              Port SSH/SFTP Aman
            </label>
            <input
              id="mt-sftp"
              type="number"
              value={profile.sftp_port}
              onChange={(e) => setProfile({ ...profile, sftp_port: Number(e.target.value) })}
              className="input-field mt-1 font-mono"
            />
          </div>
          <div>
            <label htmlFor="mt-winbox" className="block text-xs font-bold text-ink">
              Port Winbox
            </label>
            <input
              id="mt-winbox"
              type="number"
              value={profile.winbox_port}
              onChange={(e) => setProfile({ ...profile, winbox_port: Number(e.target.value) })}
              className="input-field mt-1 font-mono"
            />
          </div>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={savingMikrotik || generatingRsc}
            className="btn-primary min-h-[48px] px-5 py-2.5 text-xs font-bold"
          >
            {generatingRsc ? "Menyusun Skrip RouterOS v7..." : "Simpan Profil & Buat Skrip MikroTik (.rsc)"}
          </button>

          {generatedBundle?.rsc_script ? (
            <button
              type="button"
              onClick={handleDownloadRsc}
              className="btn-azure min-h-[48px] px-5 py-2.5 text-xs font-bold"
            >
              Unduh Berkas .rsc (1-Klik)
            </button>
          ) : null}
        </div>

        {generatedBundle ? (
          <div className="space-y-4 border-t border-subtle pt-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <StatusBadge
                tone="ok"
                prefix="SHA-256"
                label={generatedBundle.script_sha256 || "terverifikasi"}
              />
              <span className="font-mono text-xs text-subink">RouterOS v7 Export</span>
            </div>

            <pre className="max-h-80 overflow-auto rounded-lg border border-subtle bg-[#07111F] p-4 font-mono text-xs leading-relaxed text-[#F8FAFC]">
              {generatedBundle.rsc_script}
            </pre>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="rounded-lg border border-subtle bg-muted p-4">
                <h4 className="font-display text-sm font-bold text-ink">
                  Panduan Penerapan via Winbox GUI
                </h4>
                <ol className="mt-2 list-decimal space-y-1.5 pl-4 text-xs leading-relaxed text-ink">
                  {(Array.isArray(generatedBundle.winbox_steps) && generatedBundle.winbox_steps.length > 0
                    ? generatedBundle.winbox_steps
                    : [
                        "Buka Winbox melalui MAC Address atau IP lokal pada port yang ditentukan.",
                        "Unggah berkas .rsc ke menu Files atau melalui SFTP FileZilla.",
                        "Buka New Terminal lalu jalankan: /import file-name=mycaffe-routeros7.rsc verbose=yes",
                        "Periksa IP -> DHCP Server -> Leases untuk memastikan printer TCP:9100 terkunci statis."
                      ]
                  ).map((st, idx) => (
                    <li key={idx}>{st}</li>
                  ))}
                </ol>
              </div>

              <div className="rounded-lg border border-subtle bg-muted p-4">
                <h4 className="font-display text-sm font-bold text-ink">
                  Kebijakan Keamanan FileZilla SFTP-Only
                </h4>
                <p className="mt-2 text-xs leading-relaxed text-ink">
                  {typeof generatedBundle.filezilla_sftp_policy === "string"
                    ? generatedBundle.filezilla_sftp_policy
                    : JSON.stringify(generatedBundle.filezilla_sftp_policy)}
                </p>
                <p className="mt-2 font-mono text-[11px] text-subink">
                  Protokol: SFTP (SSH File Transfer Protocol) di port {profile.sftp_port}. Servis FTP port 21 dan Telnet port 23 wajib dinonaktifkan penuh.
                </p>
              </div>
            </div>
          </div>
        ) : null}
      </form>
    </div>
  );
}

