package com.starmusic.news;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    public record Article(long id, String title, String category, String summary,
                          String content, String source, String author,
                          String imageUrl, List<String> imageUrls,
                          String publishedAt, boolean breaking) {

        public Article(long id, String title, String category, String summary,
                       String content, String source, String author,
                       String imageUrl, String publishedAt, boolean breaking) {
            this(id, title, category, summary, content, source, author,
                    imageUrl, List.of(), publishedAt, breaking);
        }
    }

    private static final List<Article> ARTICLES = List.of(
            new Article(1, "星光全球娛樂台年度演唱會官宣 12/31 台北小巨蛋開唱", "娛樂",
                    "星光全球娛樂台今（24）日宣布，年度壓軸演唱會《星光之夜》將於跨年夜登場，卡司囊括華語樂壇天王天后。",
                    "星光全球娛樂台今日正式宣布，年度壓軸演唱會《星光之夜》將於 12 月 31 日晚間在台北小巨蛋開唱。主辦單位透露，本次卡司囊括華語樂壇天王天后，並將透過星光全球娛樂台全平台同步直播，預計吸引全球超過千萬觀眾收看。",
                    "星光全球娛樂台", "記者林小星", "/assets/news/concert.jpg",
                    "2026-09-24T09:00:00", true),
            new Article(2, "《乘風2026》總決賽收視創新高 成團名單出爐", "影視",
                    "選秀節目《乘風2026》總決賽收視率突破 5.2，七人女團名單正式公布。",
                    "選秀節目《乘風2026》昨晚迎來總決賽，收視率突破 5.2 創下本季新高。經過三個月的激烈角逐，最終七人成團名單正式公布，成團夜微博話題閱讀量突破 30 億。",
                    "星光全球娛樂台", "記者陳星光", "/assets/news/show.jpg",
                    "2026-09-23T22:30:00", true),
            new Article(3, "亞洲電台大賞揭曉 星光流行網蟬聯年度最佳電台", "音樂",
                    "亞洲電台大賞昨日頒獎，星光流行網連續第三年獲得年度最佳音樂電台殊榮。",
                    "亞洲電台大賞頒獎典禮昨日於新加坡舉行，星光流行網連續第三年獲得「年度最佳音樂電台」殊榮，DJ Stella 同時拿下最受歡迎主持人獎。",
                    "星光全球娛樂台", "記者王都會", "/assets/news/radio-award.jpg",
                    "2026-09-23T18:00:00", false),
            new Article(4, "串流平台大戰開打 獨家內容成決勝關鍵", "科技",
                    "各大串流平台紛紛加碼原創內容投資，分析師預估明年市場規模將突破千億。",
                    "隨著串流平台競爭白熱化，各平台紛紛加碼原創內容投資。市場分析師預估，亞太區串流市場規模明年將突破千億美元，獨家戲劇與綜藝內容成為決勝關鍵。",
                    "星光全球娛樂台", "記者李科技", "/assets/news/streaming.jpg",
                    "2026-09-22T14:20:00", false),
            new Article(5, "《長街長》完結篇網友淚崩 續集確認明年開拍", "影視",
                    "古裝大戲《長街長》播出完結篇，製作方同步宣布續集將於明年開拍。",
                    "古裝權謀大戲《長街長》昨晚播出完結篇，結局反轉讓網友淚崩。製作方同步宣布續集《長街長·歸途》將於明年春季開拍，原班人馬回歸。",
                    "星光全球娛樂台", "記者張戲劇", "/assets/news/drama.jpg",
                    "2026-09-22T23:00:00", false),
            new Article(6, "秋季美妝趨勢：星光妝容席捲時尚圈", "生活",
                    "今年秋季美妝趨勢以閃耀星光元素為主軸，各大品牌推出限定眼影盤。",
                    "今年秋季美妝趨勢以閃耀星光元素為主軸，各大品牌紛紛推出限定眼影盤與高光產品。時尚雜誌指出，帶有細緻珠光的香檳金色系將成為本季主流。",
                    "星光全球娛樂台", "記者蘇時尚", "/assets/news/beauty.jpg",
                    "2026-09-21T11:00:00", false),
            new Article(7, "虛擬偶像演唱會票房破億 AI 歌手成新藍海", "科技",
                    "虛擬偶像團體「星塵少女」首場全息演唱會票房突破一億元，AI 歌手市場備受關注。",
                    "虛擬偶像團體「星塵少女」首場全息投影演唱會票房突破一億元，創下虛擬演出新紀錄。業界認為 AI 歌手與虛擬偶像將成為娛樂產業下一波藍海市場。",
                    "星光全球娛樂台", "記者李科技", "/assets/news/virtual-idol.jpg",
                    "2026-09-20T16:45:00", false),
            new Article(8, "星光電子雜誌十週年 推出數位典藏版", "娛樂",
                    "星光電子雜誌歡慶十週年，推出收錄歷年經典封面的數位典藏版供訂戶免費下載。",
                    "星光電子雜誌歡慶創刊十週年，宣布推出收錄歷年 120 期經典封面的數位典藏版，訂戶可免費下載收藏，並同步推出十週年紀念專刊。",
                    "星光全球娛樂台", "記者林小星", "/assets/news/magazine.jpg",
                    "2026-09-19T10:00:00", false),
            new Article(9, "海饌宴會館盛大開幕 打造板橋餐飲宴會新地標", "活動",
                    "「海饌宴會館」9月21日於新北市板橋區盛大開幕，占地550坪、設10間獨立包廂及大型宴會空間，提供桌菜、港式點心、下午茶與歡唱服務。",
                    """
                    【新北市訊】位於新北市板橋區重慶路247號2樓的「海饌宴會館」，於民國115年9月21日中午11時舉行開幕典禮。剪綵貴賓包括國際獅子會300複合區國際理事邱銘乾博士、300B 2區前總監徐淑珍、新北市議員曾煥嘉、板橋廣德里里長周建和及板橋振興里里長柯仁傑，與各界嘉賓共同見證海饌正式啟航。

                    海饌宴會館占地約550坪，設有10間獨立包廂及大型宴會空間，提供桌菜、單點料理、港式點心、下午茶及歡唱服務，適合婚宴、壽宴、公司聚餐、社團會議與各類活動。

                    運營總監林均霖教授受訪表示：「海饌不只是一間餐廳，更是一個讓親友相聚、社團交流及公益活動都能自在舉辦的平台。我們將以美食連結人情、以歌聲傳遞歡樂，用真誠服務每一位顧客，讓海饌成為板橋充滿溫度的新地標。」

                    未來，海饌宴會館將持續結合餐飲、宴會、歌唱、藝文及公益活動，為民眾打造多元、歡樂且溫馨的聚會空間。

                    地址：新北市板橋區重慶路247號2樓
                    訂位專線：02-8952-6168
                    LINE ID：@616mrucc
                    """,
                    "星光全球娛樂台", "記者綜合報導", "/media/haizan-opening-1.jpg",
                    List.of("/media/haizan-opening-1.jpg", "/media/haizan-opening-5.jpg",
                            "/media/haizan-opening-3.jpg", "/media/haizan-opening-2.jpg",
                            "/media/haizan-opening-4.jpg"),
                    "2026-09-24T10:30:00", false),
            new Article(10, "慈霖圓歌藝文化推廣協會第一屆三次會員大會暨新歌發表會", "活動",
                    "慈霖圓歌藝文化推廣協會9月20日於台中舉辦第一屆第三次會員大會暨新歌發表會，歌手林上皓主持並演唱，亞洲網紅林子宸獻唱新台語歌《哈尼》，北中南歌唱界齊聚一堂。",
                    """
                    【台中訊】2026年9月20日，慈霖圓歌藝文化推廣協會於台中隆重舉辦「第一屆第三次會員大會暨新歌發表會」，現場星光雲集，由歌手林上皓擔任主持並同台演唱，眾多新發表歌手陳志雄、李家鋐、吳蕙君、林秀霞、蔡睿成、千樂等歌聲悠揚，來自北中南各地的歌唱界人士及眾多歌唱協會代表齊聚一堂，共同見證這場充滿音樂與交流的盛會。

                    慈霖圓歌藝文化推廣協會創會長林涵霖表示，對於此次活動能夠順利舉行深感感恩與榮幸。除了各地歌手及會員熱情參與，也特別感謝北中南眾多歌唱協會及音樂界好友大力支持，讓本次會員大會暨新歌發表會更加精彩，也展現歌唱同好之間彼此交流、相互支持的深厚情誼。

                    本次活動由「星光全球娛樂台」特別報導，並邀請星光之星亞洲網紅林子宸擔任現場採訪嘉賓，與新歌發表歌手進行精彩訪談，分享音樂創作、演藝歷程以及新作品背後的故事，為活動增添更多媒體焦點。

                    活動現場，亞洲網紅林子宸也帶來全新台語歌曲《哈尼》精彩演唱，舞台氣氛熱烈，獲得現場來賓熱情掌聲。除了林子宸之外，多位歌手也接力登台演出，以動人的歌聲與精彩舞台演出，為現場帶來一首又一首精彩歌曲。

                    此次盛會也獲得各界貴賓蒞臨祝賀，包括朋霖集團總裁高千惠、寶島舞王王英宗老師、星光總台長盧照仁、陳志雄老師、楊彩藝老師、心花舞蹈黃淑琴老師等貴賓親臨現場，共同向慈霖圓歌藝文化推廣協會表達祝賀，並為活動增添更多光彩。

                    慈霖圓歌藝文化推廣協會長期致力於推廣歌藝文化、促進音樂交流與凝聚歌唱同好。本次第一屆第三次會員大會結合新歌發表，不僅是會員交流的重要活動，也是台灣歌唱文化持續發展與傳承的具體展現。

                    在滿滿掌聲與祝福中，本次活動圓滿落幕。主辦單位也期盼未來持續透過更多音樂活動，串聯北中南歌唱界力量，讓歌聲傳遞熱情、讓音樂凝聚情誼，共同為台灣歌藝文化寫下更多精彩篇章。

                    （星光全球娛樂台 特別報導）
                    """,
                    "星光全球娛樂台", "記者綜合報導", "/media/cilin-assembly-1.jpg",
                    List.of("/media/cilin-assembly-1.jpg", "/media/cilin-assembly-2.jpg",
                            "/media/cilin-assembly-3.jpg", "/media/cilin-assembly-4.jpg"),
                    "2026-09-20T18:00:00", false),
            new Article(11, "各界菁英大提琴師生音樂會震撼演出 國台交團長歐陽慧剛親臨盛讚", "音樂",
                    "「田老師音樂工作坊」創辦人田方妮帶領工程師、教授與企業家學員登台演出大提琴音樂會，國立臺灣交響樂團團長歐陽慧剛親臨，盛讚學員「展現非凡音樂層次」。",
                    """
                    【星光訊】一場顛覆大眾對業餘音樂成果展想像的「大提琴師生音樂會」，6月7日於台北溫馨落幕。舞台上抱著大提琴、散發自信光芒的演奏者們，平時的身分並非音樂系畢業，而是來自社會各領域的頂尖菁英——包含工程師、教授、公司高階主管與企業家。這群白天在各自領域獨當一面的強者，在空閒撥出時間苦練，當晚一開弓，細膩且熱情洋溢的樂章震撼全場，更吸引了現任國立臺灣交響樂團（NTSO）團長歐陽慧剛老師親自蒞臨，並給予極高評價。

                    顛覆傳統古典樂高牆 科技菁英與企業家的「斜槓音樂夢」

                    主辦這場音樂會的「田老師音樂工作坊」創辦人田方妮老師表示，許多成人在諮詢時常卡在「工作太忙、壓力大、年紀不小」等擔憂。而舞台上這群在夾縫中擠出時間練習的學員，就是最好的答案。一首短短三分鐘的曲子背後，承載的是他們在結束高強度工作後、在深夜裡依然為自己保留的熱情。

                    田方妮老師感性地說：「我的角色不只是一位教 Do-Re-Mi 的大提琴老師，更是一個『幫學員造夢的人』。」正因為理解現代人在生活中承載的壓力與時間的珍貴，田老師打破傳統枯燥的磨練路線，改以細緻的合奏練習、精緻的舞台情境營造（包含親自設計投影影片與側拍記錄），將舞台的細膩度、謝幕的優雅與學員平時在職場上的自信完美結合，讓練琴與演出成為最頂級的生活美學與紓壓享受。

                    國家級大師驚艷！國台交團長歐陽慧剛：沒想到業餘學員能有此細膩度

                    音樂會的最高潮，莫過於臺灣古典樂界巨擘、國立臺灣交響樂團（NTSO）團長歐陽慧剛老師的親自出席。歐陽團長在全程觀賞演出後，神情滿是興奮與激動，並在會後給予這群「斜槓音樂家」極高的專業肯定。

                    歐陽慧剛團長讚嘆地表示，他非常驚訝一群來自各行各業、從零開始的成人學習者，竟然能在舞台上展現出如此細膩的音樂處理、豐富的合奏層次，以及對音樂毫無保留的專注與熱情。歐陽團長現場給予大力的勉勵與肯定，這份來自國家級大師的讚賞，無疑是全體學員最耀眼的勳章。

                    這場結合了美學、科技與跨界菁英的音樂盛會，不僅向大眾證明了「完成勝過完美」的音樂教育哲學，更展示了只要有對的引路人，每個人心中的音樂夢，都能綻放出專業且耀眼的光芒。

                    【關於田老師音樂工作坊】由專業大提琴家田方妮老師創辦，致力於推廣大提琴，勵志拆掉專業的圍牆，把演奏大提琴專業的技術，翻譯成身體能聽懂的語言。
                    """,
                    "星光全球娛樂台", "記者綜合報導", "/media/cello-concert-1.jpg",
                    List.of("/media/cello-concert-1.jpg", "/media/cello-concert-2.jpg",
                            "/media/cello-concert-3.jpg", "/media/cello-concert-4.jpg",
                            "/media/cello-concert-5.jpg", "/media/cello-concert-6.jpg"),
                    "2026-06-08T12:00:00", false),
            new Article(12, "大嬸婆國際同濟會第十屆會員大會圓滿舉行 榮耀同濟共創新局", "活動",
                    "新北市大嬸婆同濟會6月6日於新北市客家文化園區客家美食主藝餐廳舉辦第十屆會員大會，以「榮耀同濟，共創新局；團結努力，向前行」為主題，各界貴賓雲集。",
                    """
                    【星光全球娛樂台／盧照仁、鮑戈新北市聯合報導】2026年6月6日（星期六）下午四時，新北市大嬸婆同濟會於新北市客家文化園區客家美食主藝餐廳隆重舉辦「第十屆會員大會」。與會的會員及各界貴賓齊聚一堂，共同見證會務成果，展望未來發展方向。

                    本次大會以「榮耀同濟，共創新局；團結努力，向前行」為主題，展現同濟人服務社會、關懷弱勢及凝聚團隊向心力的精神。會中除進行年度會務報告、財務報告及各項議案討論外，還回顧了過去一年在公益服務、社群關懷及慈善活動中取得的豐碩成果。

                    特別致意：區主席黃進礐、副主席彭振雄、區執行長蔡尚林、第52屆聯誼會會長同學、林口會、強大會母會、創會長蕭韻華、第4屆會長游秀珠、第8屆會長林建宏、第9屆會長林俊宏、候任會長蕭檉譁（一副）、陳耀堂（二副）、秘書長彭思溫。

                    會中對歷屆會長、顧問及全體會員的辛勤付出表示衷心感謝，強調透過會員大會的召開，不僅增進會員間的交流與情誼，更凝聚了未來發展的共識，攜手開創嶄新局面。

                    值得一提的是，為了響應社群的需求並推進地方發展，大嬸婆國際同濟會理事陳芃特別參加了此次大會。作為新北市泰山區同榮里里長選舉候選人，陳芃表示，透過參與同濟會的活動，能夠與更多的社群人士交流，學習大家的經驗和需求，共同造福社會。他強調，未來將以熱忱與責任心，積極投入里長工作，推動同榮里的繁榮與和諧。

                    與會的貴賓對大嬸婆同濟會多年來積極投入地方公益、熱心參與社會服務給予高度肯定，並期許未來能夠持續秉持同濟精神，發揮影響力，造福更多需要幫助的民眾。新北市大嬸婆同濟會重申，未來會持續秉持「服務社會、關懷人群」的宗旨，凝聚會員力量，深化公益服務，共同打造溫暖祥和的社會，朝向更美好的未來邁進。

                    貴賓雲集，特邀出席：國際嘎檔巴慧吉祥大活佛（榮譽會長）、中華文化藝術總院院長暨中華學術文教基金會董事長高崇雲教授、中華學術文教基金會王海倫執行長、行政院青輔會前主委李永騰教授、台灣非營利組織聯合總會榮譽總會長張連興（榮譽會長）、陸軍中將周康生伉儷孫新梅、東亞藝術研究會榮譽理事長蔡賢謀、海峽兩岸邀請展總策展人王美美教授、亞太陶藝文創協會創會理事長楊乃翰、華夏藝文學群英會理事長林旺錢、藝術家黃明勝老師、台北市書畫美術公會副理事長許玉惠、書法教師陳秋華、領袖國際有限公司執行長蔡孟蒨、太極拳國手李岳洋、珠寶協會副理事長阮氏秋霞、星光全球娛樂台創辦人盧照仁總台長、立錡食品有限公司呂秉澤總經理、烏金炭皂達人高智輝總經理、光宇能量帽周光宇博士、意考斯國際企業王懿謙執行長、國際互助關懷協會劉以琳理事長、依菲ㄦ曼陳庥妘執行長、昱盛國際生技有限公司王怡盛執行長、地球村生技有限公司陳秋澄總經理、金字塔能量股份有限公司范鳴峰總經理、台灣牛樟芝集團彭子席總經理、活元動力黃光啟執行長、永富旅行社方金鋐董事長、李淑君護理師。

                    最後，在全體會員的共同努力與支援下，本次大會圓滿成功。讓我們攜手共進，繼續推動大嬸婆同濟會的發展，為社會帶來更多的關懷與溫暖。「榮耀同濟，共創新局；團結努力，向前行」，期待在未來的日子裡，與各位會員一起，為服務社會貢獻更多的力量，共同譜寫新篇章。祝福大嬸婆同濟會在未來的日子裡持續蓬勃發展，成為更多需要幫助的人們的支援與依靠！

                    更多精彩照片：https://photos.app.goo.gl/SmVrW4BPtnKN9vLU9
                    """,
                    "星光全球娛樂台", "盧照仁、鮑戈", "/media/dabenpo-assembly-1.jpg",
                    List.of("/media/dabenpo-assembly-1.jpg", "/media/dabenpo-assembly-2.jpg",
                            "/media/dabenpo-assembly-3.jpg", "/media/dabenpo-assembly-4.jpg",
                            "/media/dabenpo-assembly-5.jpg", "/media/dabenpo-assembly-6.jpg",
                            "/media/dabenpo-assembly-7.jpg", "/media/dabenpo-assembly-8.jpg"),
                    "2026-06-07T12:00:00", false)
    );

    public record ArticleRequest(String title, String category, String summary,
                                 String content, String source, String author,
                                 String imageUrl, List<String> imageUrls,
                                 Boolean breaking) {
    }

    private static final long MANAGED_ID_BASE = 10000;
    private static final int MAX_TITLE = 200;
    private static final int MAX_SHORT = 100;
    private static final int MAX_SUMMARY = 500;
    private static final int MAX_CONTENT = 20000;

    private final NewsArticleRepository articles;

    public NewsController(NewsArticleRepository articles) {
        this.articles = articles;
    }

    @GetMapping
    public List<Article> list(@RequestParam(required = false) String category) {
        return all()
                .filter(a -> category == null || a.category().equals(category))
                .sorted(Comparator.comparing(Article::publishedAt).reversed())
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> get(@PathVariable long id) {
        return all()
                .filter(a -> a.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/breaking")
    public List<Article> breaking() {
        return all().filter(Article::breaking).toList();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return all().map(Article::category).distinct().toList();
    }

    @GetMapping("/manage")
    public List<Article> managed(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return articles.findAll().stream()
                .sorted(Comparator.comparing(NewsArticleEntity::getId).reversed())
                .map(this::fromEntity)
                .toList();
    }

    @PostMapping("/manage")
    public Article create(@RequestBody(required = false) ArticleRequest req,
                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        if (req == null || req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題必填");
        }
        if (req.title().length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題過長");
        }
        checkLen(req.category(), MAX_SHORT, "分類");
        checkLen(req.source(), MAX_SHORT, "來源");
        checkLen(req.author(), MAX_SHORT, "作者");
        checkLen(req.summary(), MAX_SUMMARY, "摘要");
        checkLen(req.content(), MAX_CONTENT, "內文");

        NewsArticleEntity e = new NewsArticleEntity();
        e.setTitle(req.title().trim());
        e.setCategory(req.category());
        e.setSummary(req.summary());
        e.setContent(req.content());
        e.setSource(req.source());
        e.setAuthor(req.author());
        e.setImageUrl(req.imageUrl());
        e.setImageUrls(req.imageUrls() == null ? null : String.join("\n", req.imageUrls()));
        e.setBreaking(Boolean.TRUE.equals(req.breaking()));
        return fromEntity(articles.save(e));
    }

    @DeleteMapping("/manage/{id}")
    public ResponseEntity<Void> deleteManaged(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        long dbId = id - MANAGED_ID_BASE;
        if (!articles.existsById(dbId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "內容不存在或為內建資料");
        }
        articles.deleteById(dbId);
        return ResponseEntity.noContent().build();
    }

    private Stream<Article> all() {
        return Stream.concat(
                ARTICLES.stream(),
                articles.findAll().stream().map(this::fromEntity));
    }

    private Article fromEntity(NewsArticleEntity e) {
        List<String> urls = e.getImageUrls() == null || e.getImageUrls().isBlank()
                ? List.of()
                : List.of(e.getImageUrls().split("\\R"));
        return new Article(MANAGED_ID_BASE + e.getId(), e.getTitle(), e.getCategory(),
                e.getSummary(), e.getContent(), e.getSource(), e.getAuthor(),
                e.getImageUrl(), urls, e.getPublishedAt(), e.isBreaking());
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    private void checkLen(String v, int max, String label) {
        if (v != null && v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "過長");
        }
    }
}
