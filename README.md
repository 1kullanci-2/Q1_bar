# Medya Paneli

Çalan müziği (Spotify, YouTube Music vb.) kilit ekranında cam görünümlü bir panelde gösteren Android uygulaması.

- Üstte büyük kart: kapak, şarkı adı, sanatçı, albüm, çalan uygulama
- Altta ince geniş kart: süre çubuğu, geri / oynat-durdur / ileri
- Veriyi bildirim erişimi ile medya oturumlarından okur

## Derleme

GitHub'a gönderince **Actions** sekmesinde "APK derle" çalışır. Bittiğinde çalıştırmanın altındaki
**medya-paneli-apk** dosyasını indir, içinden `app-debug.apk` çıkar.

## Kurulum sonrası

1. Uygulamayı aç, **Bildirim erişimi**ni aç.
2. **Diğer uygulamaların üstünde göster** iznini aç (Android 10+).
3. **Pil kısıtlaması**ndan uygulamayı kısıtlamasız yap (Huawei/EMUI için önemli).
4. Müzik çal, ekranı kapat, tekrar aç.
