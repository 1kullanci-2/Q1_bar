# Medya Paneli

Çalan müziği (Spotify, YouTube Music vb.) kilit ekranında ve ana ekranda gösteren Android uygulaması.

- Kapak odaklı panel: kapak çerçevesi kapağın oranına göre şekillenir, renkler kapaktan gelir
- Kilit ekranı: alt çubuk (dokununca panel açılır), tam panel ya da kapalı
- Büyük ince saat ve kapağın rengini alan tarih hapı
- Ana ekran widget'ı: kapak, şarkı, sanatçı, uygulama simgesi, önceki / oynat-durdur / sonraki
- Veriyi bildirim erişimi ile medya oturumlarından okur

## Derleme

GitHub'a gönderince **Actions** sekmesinde "APK derle" çalışır. Bittiğinde çalıştırmanın altındaki
**medya-paneli-apk** dosyasını indir, içinden `app-debug.apk` çıkar.

## Kurulum sonrası

1. Uygulamayı aç, **Bildirim erişimi**ni aç.
2. **Diğer uygulamaların üstünde göster** iznini aç.
3. Pil kısıtlamasını kaldır ve (Huawei) başlatma ayarlarında Otomatik başlat, İkincil başlat, Arka planda çalış seçeneklerini aç.
4. Müzik çal, ekranı kapat, tekrar aç.
