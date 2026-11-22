# Medya Paneli

Çalan müziği (Spotify, YouTube Music vb.) kilit ekranında ve ana ekranda gösteren Android uygulaması.

- Kilit ekranı paneli: iki çerçeve (üstte kapak + şarkı bilgisi, altta süre çubuğu ve düğmeler), renkler kapaktan gelir
- Kapak çerçevesi kapağın oranına göre şekillenir
- Uygulama adı kutucuğuna dokununca kilit açtırıp müzik uygulamasına gider
- Saat için 3 yazı tipi (Outfit, Fraunces, Space Grotesk), boyut, tarih ve 12/24 saat ayarı
- İnce ana ekran widget'ı: kapak, şarkı, sanatçı, süre çubuğu, karıştır / tekrar, önceki / oynat-durdur / sonraki
- Veriyi bildirim erişimi ile medya oturumlarından okur

## Derleme ve güncelleme

GitHub'a gönderince **Actions** sekmesinde "APK derle" çalışır. `KEYSTORE_BASE64` gizli kaydı varsa APK sabit anahtarla
imzalanır ve **Releases** altında yeni sürüm olarak yayınlanır (sürüm numarası = çalıştırma sırası). Uygulama kendi içinden
son sürümü denetler, indirir ve kurar; izinler ve ayarlar korunur. Bunun için depo herkese açık olmalıdır.

## Kurulum sonrası

İlk açılışta kurulum sihirbazı izinleri tek tek anlatır ve doğru ayar ekranını açar
(bildirim erişimi, üstünde göster, pil, Huawei başlatma). Sonradan ana ekrandan yeniden çalıştırılabilir.

## Kurulum sayacı (isteğe bağlı)

`worker/index.js` bir Cloudflare Worker'dır (KV bağlaması: `KV`). Adresi `Config.COUNTER_URL` içine yazınca uygulama
ilk açılışta anonim bir kimlik bildirir. Sayıyı görmek için Hakkında ekranında sürüm yazısına 7 kez dokun.

## Lisanslar

Yazı tipleri SIL Open Font License altındadır: Outfit, Fraunces, Space Grotesk (bkz. `licenses/`).
