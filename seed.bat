@echo off
echo 🚀 Dang nap 50 dia danh vao Firebase...
curl -X PUT -d "@app/src/main/assets/data/seed_data.json" "https://travelmate-app-7bcbc-default-rtdb.asia-southeast1.firebasedatabase.app/.json"
echo.
echo ✅ Hoan tat! Kiem tra Firebase Console ngay.
pause
