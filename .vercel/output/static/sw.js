self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = event.notification.data?.url || "/daily";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if ("focus" in client) {
          client.focus();
          if ("navigate" in client) client.navigate(url);
          return;
        }
      }
      if (self.clients.openWindow) return self.clients.openWindow(url);
    }),
  );
});

self.addEventListener("periodicsync", (event) => {
  if (event.tag === "mizan-daily") {
    event.waitUntil(
      self.registration.showNotification("مرور روزانه میزان", {
        body: "سه ماده قانون مدنی، تجارت و آیین دادرسی امروز آماده است.",
        lang: "fa",
        dir: "rtl",
        icon: "/icon-192.png",
        tag: "mizan-periodic",
        data: { url: "/daily" },
      }),
    );
  }
});

self.addEventListener("message", (event) => {
  const data = event.data;
  if (data && data.type === "NOTIFY") {
    event.waitUntil(
      self.registration.showNotification(data.title, data.options || {}),
    );
  }
});
