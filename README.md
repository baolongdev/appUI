# 📦 HƯỚNG DẪN RELEASE PHIÊN BẢN MỚI

## 🎯 TL;DR - Quick Steps

```bash
# 1. Update version
# Edit app/build.gradle.kts → AppVersion

# 2. Commit
git add .
git commit -m "Release v1.1.0"
git push

# 3. Create tag
git tag v1.1.0
git push origin v1.1.0

# 4. Wait for GitHub Actions to build
# 5. Download APK từ GitHub Releases
```


***

## 📋 CHI TIẾT TỪNG BƯỚC

### **Bước 1: Update Version Number**

Mở file `app/build.gradle.kts`, tìm và sửa:

```kotlin
// ✅ CHỈNH 3 SỐ NÀY
object AppVersion {
    const val major = 1      // ← Tăng khi có breaking changes
    const val minor = 1      // ← Tăng khi thêm tính năng mới
    const val patch = 0      // ← Tăng khi fix bugs
    const val code = major * 10000 + minor * 100 + patch
    const val name = "$major.$minor.$patch"
}
```

**Quy tắc đánh số:**

- **Patch** (1.0.0 → 1.0.1): Bug fixes, cải tiến nhỏ
- **Minor** (1.0.1 → 1.1.0): Tính năng mới, không breaking
- **Major** (1.1.0 → 2.0.0): Breaking changes, UI redesign

***

### **Bước 2: Test Local Build**

```bash
# Build để check lỗi
./gradlew clean assembleRelease

# Nếu build thành công → tiếp tục
# Nếu có lỗi → fix trước khi commit
```


***

### **Bước 3: Commit Code**

```bash
# Check files changed
git status

# Add tất cả thay đổi
git add .

# Commit với message rõ ràng
git commit -m "Release v1.1.0

- Thêm tính năng X
- Fix bug Y
- Cải thiện performance Z"

# Push lên GitHub
git push origin main
```


***

### **Bước 4: Create và Push Tag**

```bash
# Tạo tag (phải khớp với version trong build.gradle.kts)
git tag v1.1.0

# Push tag → Trigger GitHub Actions
git push origin v1.1.0
```

**⚠️ CHÚ Ý:**

- Tag phải có prefix `v` (v1.1.0, không phải 1.1.0)
- Tag phải khớp với version trong `AppVersion`

***

### **Bước 5: Monitor GitHub Actions**

1. Vào: `https://github.com/YOUR_USERNAME/appUI/actions`
2. Click vào workflow "Build and Release APK"
3. Đợi ~5-10 phút
4. Status:
    - 🟡 **Running**: Đang build
    - 🟢 **Success**: Build thành công
    - 🔴 **Failed**: Có lỗi → Check logs

***

### **Bước 6: Verify Release**

Khi workflow thành công:

1. Vào: `https://github.com/YOUR_USERNAME/appUI/releases`
2. Kiểm tra release mới: `v1.1.0`
3. Download APK để test
4. Check file `update.json` đã được update

***

### **Bước 7: Test Update Trong App**

1. Install APK version cũ (v1.0.0) trên điện thoại
2. Mở app
3. Popup sẽ hiện: "Phiên bản mới có sẵn! v1.1.0"
4. Chấm đỏ xuất hiện trên icon Settings
5. Click "Cập nhật" → Download và install v1.1.0

***

## 🐛 TROUBLESHOOTING

### **Lỗi: Workflow không chạy**

```bash
# Check tag đã push chưa
git ls-remote --tags origin

# Nếu không có tag → push lại
git push origin v1.1.0
```


### **Lỗi: Build failed**

```bash
# Check logs tại:
# GitHub → Actions → Click vào failed workflow

# Common fixes:
# 1. Keystore secrets chưa đúng → Re-add GitHub Secrets
# 2. Syntax error trong code → Fix và commit lại
# 3. Signing error → Kiểm tra keystore.properties
```


### **Lỗi: APK không install được**

```bash
# Enable "Install from Unknown Sources"
# Settings → Security → Unknown Sources → ON

# Hoặc khi install, click "Settings" và bật quyền
```


***

## 📝 QUICK REFERENCE

### Version Format:

```
v1.0.0 → Major.Minor.Patch
  │ │ │
  │ │ └─ Bug fixes (0, 1, 2, ...)
  │ └─── New features (0, 1, 2, ...)
  └───── Breaking changes (1, 2, 3, ...)
```


### Git Commands:

```bash
# Commit + Tag + Push
git add .
git commit -m "Release vX.Y.Z"
git push
git tag vX.Y.Z
git push origin vX.Y.Z

# Delete tag nếu nhầm
git tag -d vX.Y.Z
git push origin :refs/tags/vX.Y.Z
```


### Files to Update:

```
✅ app/build.gradle.kts → AppVersion (required)
✅ Release notes trong workflow (optional)
```


***

## 🎯 BEST PRACTICES

1. **Luôn test local trước khi push tag**

```bash
./gradlew assembleRelease
```

2. **Viết release notes rõ ràng**
    - Tính năng mới gì?
    - Fix bug gì?
    - Breaking changes?
3. **Version numbering nhất quán**
    - Commit: "Release v1.1.0"
    - Tag: v1.1.0
    - AppVersion: 1.1.0
4. **Backup keystore**
    - Lưu `release.keystore` và password an toàn
    - Mất keystore = không update được app
5. **Test update flow**
    - Test với app version cũ
    - Verify popup hiện đúng
    - Download và install thành công

***

## 📞 SUPPORT

- GitHub Issues: `https://github.com/YOUR_USERNAME/appUI/issues`
- Workflow logs: `https://github.com/YOUR_USERNAME/appUI/actions`
- Release page: `https://github.com/YOUR_USERNAME/appUI/releases`

***

**Lưu file này tại:** `D:\JOB\MOBILE\appUI\RELEASE_GUIDE.md`

Done! 🎉 Giờ bạn có hướng dẫn đầy đủ để release phiên bản mới! 🚀✨

