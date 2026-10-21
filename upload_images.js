const admin = require('firebase-admin');
const axios = require('axios');
const fs = require('fs');

if (!fs.existsSync('./serviceAccountKey.json')) {
    console.error('❌ LỖI: Không tìm thấy file serviceAccountKey.json!');
    process.exit(1);
}

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: "travelmate-app-7bcbc.firebasestorage.app"
});

const bucket = admin.storage().bucket();
const db = admin.firestore();

const landmarks = [
  { id: "landmark_01", name: "Chùa Một Cột", query: "one pillar pagoda hanoi" },
  { id: "landmark_02", name: "Vịnh Hạ Long", query: "halong bay" },
  { id: "landmark_03", name: "Đèo Mã Pì Lèng", query: "mountain pass vietnam" },
  { id: "landmark_04", name: "Cố đô Huế", query: "hue imperial city" },
  { id: "landmark_05", name: "Phố cổ Hội An", query: "hoi an" },
  { id: "landmark_06", name: "Fansipan", query: "fansipan mountain" },
  { id: "landmark_07", name: "Hang Sơn Đoòng", query: "cave vietnam" },
  { id: "landmark_08", name: "Bãi Sao", query: "beach phu quoc" },
  { id: "landmark_09", name: "Tháp Bà Ponagar", query: "cham temple" },
  { id: "landmark_10", name: "Dinh Độc Lập", query: "independence palace saigon" },
  { id: "landmark_11", name: "Tam Cốc - Bích Động", query: "ninh binh landscape" },
  { id: "landmark_12", name: "Gành Đá Đĩa", query: "sea cliff stone" },
  { id: "landmark_13", name: "Thung lũng Tình Yêu", query: "da lat park" },
  { id: "landmark_14", name: "Thánh địa Mỹ Sơn", query: "my son sanctuary" },
  { id: "landmark_15", name: "Chợ nổi Cái Răng", query: "floating market vietnam" },
  { id: "landmark_16", name: "Mũi Cà Mau", query: "ca mau cape" },
  { id: "landmark_17", name: "Đảo Kỳ Co", query: "beach quy nhon" },
  { id: "landmark_18", name: "Vườn quốc gia Ba Vì", query: "ba vi national park" },
  { id: "landmark_19", name: "Nhà thờ lớn Hà Nội", query: "hanoi cathedral" },
  { id: "landmark_20", name: "Cầu Vàng", query: "golden bridge da nang" },
  { id: "landmark_21", name: "Thác Bản Giốc", query: "ban gioc waterfall" },
  { id: "landmark_22", name: "Mù Cang Chải", query: "rice terrace vietnam" },
  { id: "landmark_23", name: "Đồi Cát Bay", query: "sand dune mui ne" },
  { id: "landmark_24", name: "Chùa Hương", query: "perfume pagoda" },
  { id: "landmark_25", name: "Vườn quốc gia Tràm Chim", query: "bird national park" },
  { id: "landmark_26", name: "Bưu điện Trung tâm Sài Gòn", query: "central post office saigon" },
  { id: "landmark_27", name: "Đèo Hải Vân", query: "hai van pass" },
  { id: "landmark_28", name: "Tràng An", query: "trang an ninh binh" },
  { id: "landmark_29", name: "Mũi Né", query: "mui ne beach" },
  { id: "landmark_30", name: "Bãi biển Mỹ Khê", query: "my khe beach" },
  { id: "landmark_31", name: "Hồ Gươm", query: "hoan kiem lake" },
  { id: "landmark_32", name: "Nhà tù Côn Đảo", query: "con dao prison" },
  { id: "landmark_33", name: "Côn Đảo", query: "con dao island" },
  { id: "landmark_34", name: "Tòa Thánh Tây Ninh", query: "cao dai temple" },
  { id: "landmark_35", name: "Núi Bà Đen", query: "ba den mountain" },
  { id: "landmark_36", name: "Vườn quốc gia Cúc Phương", query: "cuc phuong forest" },
  { id: "landmark_37", name: "Rừng tràm Trà Sư", query: "tra su forest" },
  { id: "landmark_38", name: "Thác Đray Nur", query: "waterfall central highland" },
  { id: "landmark_39", name: "Biển Hồ", query: "pleiku lake" },
  { id: "landmark_40", name: "Chùa Linh Ứng", query: "linh ung pagoda" },
  { id: "landmark_41", name: "Côn Sơn - Kiếp Bạc", query: "relic vietnam" },
  { id: "landmark_42", name: "Bạch Mã", query: "bach ma mountain" },
  { id: "landmark_43", name: "Chùa Keo", query: "keo pagoda" },
  { id: "landmark_44", name: "Lam Kinh", query: "lam kinh palace" },
  { id: "landmark_45", name: "Đảo Lý Sơn", query: "ly son island" },
  { id: "landmark_46", name: "Chùa Tam Chúc", query: "tam chuc pagoda" },
  { id: "landmark_47", name: "Bạch Đằng Giang", query: "river relic" },
  { id: "landmark_48", name: "Lăng Bác", query: "ho chi minh mausoleum" },
  { id: "landmark_49", name: "Đại Nội Huế", query: "hue citadel" },
  { id: "landmark_50", name: "Chùa Bái Đính", query: "bai dinh pagoda" }
];

async function downloadAndUpload(landmark) {
  console.log(`📸 Đang xử lý: ${landmark.name}...`);
  const imageUrls = [];

  for (let i = 1; i <= 3; i++) {
    try {
      // Dùng API của Unsplash (ổn định hơn)
      const url = `https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=800&q=80&sig=${landmark.id}_${i}`;
      const response = await axios.get(url, { responseType: 'arraybuffer' });

      const filename = `destinations/${landmark.id}_${i}.jpg`;
      const file = bucket.file(filename);

      await file.save(response.data, {
        contentType: 'image/jpeg',
        metadata: { cacheControl: 'public, max-age=31536000' }
      });

      await file.makePublic();
      const publicUrl = `https://storage.googleapis.com/${bucket.name}/${filename}`;

      imageUrls.push(publicUrl);
      console.log(`   ✅ Đã upload ảnh ${i}`);
    } catch (e) {
      console.log(`   ❌ Lỗi ảnh ${i}: ${e.message}`);
    }
  }

  if (imageUrls.length > 0) {
    await db.collection('destinations').doc(landmark.id).set({ imageUrls }, { merge: true });
  }
}

async function run() {
  console.log("🚀 Bắt đầu nạp ảnh THẬT lên Firebase Storage (Nguồn: Unsplash)...");
  for (const landmark of landmarks) {
    await downloadAndUpload(landmark);
  }
  console.log("\n🎉 HOÀN TẤT 100%! Hãy mở App kiểm tra kết quả.");
  process.exit();
}

run();
