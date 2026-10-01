// 正式環境（FTP 純靜態空間）：GET 請求改由 /assets/api/ 下的靜態快照檔提供
// （快照由 scripts/snapshot-api.sh 產生）；會員註冊/登入改用瀏覽器 localStorage
// 模擬（AuthService 內的靜態模式邏輯），其餘需後端的功能（留言、購物等）仍不可用。
// 若日後將後端架設到主機，把 apiBase 改成後端網址並將 staticData 改為 false，
// 再重新執行 npm run build。
export const environment = {
  apiBase: '/api',
  staticData: true
};
