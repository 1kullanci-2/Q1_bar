# Medya Paneli

Çalan müziği (Spotify, YouTube Music vb.) kilit ekranında ve ana ekranda gösteren Android uygulaması.

- Kilit ekranı paneli: iki çerçeve (üstte kapak + şarkı bilgisi, altta süre çubuğu ve düğmeler), renkler kapaktan gelir
- Kapak çerçevesi kapağın oranına göre şekillenir
- Uygulama adı kutucuğuna dokununca kilit açtırıp müzik uygulamasına gider
- Saat için 3 yazı tipi (Outfit, Fraunces, Space Grotesk), boyut, tarih ve 12/24 saat ayarı
- İnce ana ekran widget'ı: kapak, şarkı, sanatçı, süre çubuğu, karıştır / tekrar, önceki / oynat-durdur / sonraki
- Veriyi bildirim erişimi ile medya oturumlarından okur

## Derleme

GitHub'a gönderince **Actions** sekmesinde "APK derle" çalışır. Bittiğinde çalıştırmanın altındaki
**medya-paneli-apk** dosyasını indir, içinden `app-debug.apk` çıkar.

## Kurulum sonrası

1. Uygulamayı aç, **Bildirim erişimi**ni aç.
2. **Diğer uygulamaların üstünde göster** iznini aç.
3. Pil kısıtlamasını kaldır ve (Huawei) başlatma ayarlarında Otomatik başlat, İkincil başlat, Arka planda çalış seçeneklerini aç.
4. Müzik çal, ekranı kapat, tekrar aç.

## Lisanslar

Yazı tipleri SIL Open Font License altındadır: Outfit, Fraunces, Space Grotesk (bkz. `licenses/`).
