package team.cheese.controller;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import team.cheese.dao.ImgDao;
import team.cheese.dao.SaleDao;
import team.cheese.domain.ImgDto;
import team.cheese.domain.SaleDto;
import team.cheese.service.ImgService;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class test2 {

    @Autowired
    ImgService imgService;

    @Autowired
    SaleDao saleDao;

    @Autowired
    ImgDao imgDao;

//    String folderPath = "/Users/jehyeon/Desktop/Team/src/main/webapp/resources/img"; // 절대 경로 테스트 삼아 지정

    public String path(HttpServletRequest request){
        ServletContext servletContext = request.getServletContext();
        String realPath = servletContext.getRealPath("/");
        String folderPath = realPath.substring(0, realPath.indexOf("target"))+"src/main/webapp/resources/img";
        return folderPath;
    }

    @GetMapping("/test")
    public String ajax() {
        return "img/test";
    }

    //이미지 보여주기용
    @GetMapping("/display")
    public ResponseEntity<byte[]> getImage(String fileName, HttpServletRequest request){

        String folderPath = path(request);
        File file = new File(folderPath+"/"+ fileName);
        ResponseEntity<byte[]> result = null;

        try {
            HttpHeaders header = new HttpHeaders();
            header.add("Content-type", Files.probeContentType(file.toPath()));
            result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);
        }catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /* 이미지 파일 삭제 */
    @PostMapping("/deleteFile")
    public ResponseEntity<String> deleteFile(String fileName, HttpServletRequest request){
        String folderPath = path(request);

        //DB내용만 상태변화 시키기
//        System.out.println("delete : "+fileName);
        File file = null;
        try {
            file = new File(folderPath+"/" + URLDecoder.decode(fileName, "UTF-8"));
            file.delete();
            /* 썸네일 파일 삭제 */
            String originFileName = file.getAbsolutePath().replace("s_", "");
            file = new File(originFileName);
            file.delete();
        } catch(Exception e) {
            e.printStackTrace();
            return new ResponseEntity<String>("fail", HttpStatus.NOT_IMPLEMENTED);

        }
        return new ResponseEntity<String>("success", HttpStatus.OK);
    }

    @RequestMapping("/testview")
    public String viewtest(Model model){
        List<ImgDto> list = imgDao.select_s_imglist(); // 이미지서비스
//        System.out.println(list);
        model.addAttribute("list", list);
        System.out.println("뷰 이동");
        return "img/testview";
    }

    @RequestMapping("/reg_image")
    @ResponseBody
    public ResponseEntity<String> reg_img(@RequestBody ArrayList<ImgDto> imglist, HttpServletRequest request){
        if(imglist.size() == 0){
            return new ResponseEntity<String>("fail", HttpStatus.NOT_IMPLEMENTED);
        }
        try {
        String folderPath = path(request);

        boolean change = true;

        String datePath = Todaystr();

        /* 파일 저장 폴더 이름 */
        File uploadPath = new File(folderPath, datePath);

        //게시글 임의로 넣어둠 테스트를 위해서
        SaleDto saledto = new SaleDto();
        saledto.setSeller_id("user123");
        saledto.setSal_i_cd("016001005");
        saledto.setPro_s_cd("C");
        saledto.setTx_s_cd("S");
        // 거래방법 1개만 작성
        saledto.setTrade_s_cd_1("F");
        saledto.setTrade_s_cd_2("F");
        saledto.setPrice(28000);
        saledto.setSal_s_cd("S");
        saledto.setTitle("자바의 정석");
        saledto.setContents("자바의 정석 2판 팔아요.");
        saledto.setBid_cd("N");
        saledto.setPickup_addr_cd("11060710");
        saledto.setDetail_addr("회기역 1번출구 앞(20시 이후만 가능)");
        saledto.setBrand("자바의 정석");
        saledto.setReg_price(30000);
        saledto.setCheck_addr_cd(0);
        saleDao.insert_sale(saledto);

        for (ImgDto img : imglist) {
            try {
                String fname = folderPath+"/"+img.getFilert()+"/"+img.getO_name()+img.getE_name();

                File imageFile = new File(fname);

                String uploadFileName = datePath+"_"+img.getO_name()+img.getE_name();

                //미리보기이미지 경로저장
                saveDBimg(saledto.getNo(), "sale", "r", datePath, img, 78, 78);

                //썸네일 이미지
                if(change){
                    Makeimg2(imageFile, uploadPath, "s_" + uploadFileName, 292, 292);
                    saveDBimg(saledto.getNo(), "sale", "s", datePath, img, 292, 292);
                    change = false;
                }

                //본문 이미지
                Makeimg2(imageFile, uploadPath, "w_" + uploadFileName, 856, 856);
                saveDBimg(saledto.getNo(), "sale", "w", datePath, img, 856, 856);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //교차테이블 작성
        HashMap map = new HashMap();
        map.put("no", saledto.getNo());
        map.put("col_name", "sal_no");
        map.put("no_name", "no");
        map.put("cross_tb_name", "sale");
        map.put("tb_name", "sale_img");
        imgService.view_img(map);
        System.out.println("전송됨");
        } catch(Exception e) {
            e.printStackTrace();
            return new ResponseEntity<String>("fail", HttpStatus.NOT_IMPLEMENTED);
        }
        return new ResponseEntity<String>("/testview", HttpStatus.OK);
//        return "/testview";
    }

    //테스트를 위해서 사용해보는 Ajax
    @RequestMapping("/reg_image2")
    @ResponseBody
//    public String reg_img2(@RequestBody Map<String, Object> info){
    public String reg_img2(@RequestBody ArrayList<ImgDto> imglist, HttpServletRequest request){
        String folderPath = path(request);

        String datePath = Todaystr();

        boolean change = true;

        /* 파일 저장 폴더 이름 */
        File uploadPath = new File(folderPath, datePath);

        //게시글 임의로 넣어둠 테스트를 위해서
        SaleDto saledto = new SaleDto();
        saledto.setSeller_id("user123");
        saledto.setSal_i_cd("016001005");
        saledto.setPro_s_cd("C");
        saledto.setTx_s_cd("S");
        // 거래방법 1개만 작성
        saledto.setTrade_s_cd_1("F");
        saledto.setTrade_s_cd_2("F");
        saledto.setPrice(28000);
        saledto.setSal_s_cd("S");
        saledto.setTitle("자바의 정석");
        saledto.setContents("자바의 정석 2판 팔아요.");
        saledto.setBid_cd("N");
        saledto.setPickup_addr_cd("11060710");
        saledto.setDetail_addr("회기역 1번출구 앞(20시 이후만 가능)");
        saledto.setBrand("자바의 정석");
        saledto.setReg_price(30000);
        saledto.setCheck_addr_cd(0);
        saleDao.insert_sale(saledto);

//        for (ImgDto img : imglist) {
//            try {
//                String fname = folderPath+"/"+img.getFilert()+"/"+img.getO_name()+img.getE_name();
//
//                File imageFile = new File(fname);
//
//                String uploadFileName = datePath+"_"+img.getO_name()+img.getE_name();
//
//                //미리보기이미지 경로저장
//                saveDBimg(saledto.getNo(), "sale", "r", datePath, img, 78, 78);
//
//                //썸네일 이미지
//                if(change){
//                    Makeimg2(imageFile, uploadPath, "s_" + uploadFileName, 292, 292);
//                    saveDBimg(saledto.getNo(), "sale", "s", datePath, img, 292, 292);
//                    change = false;
//                }
//
//                //본문 이미지
//                Makeimg2(imageFile, uploadPath, "w_" + uploadFileName, 856, 856);
//                saveDBimg(saledto.getNo(), "sale", "w", datePath, img, 856, 856);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        System.out.println("hello : "+saledto);
        return "/testview";
//        return list;
    }

    //----------------test--------------
    //이미지 업로드 과정
    @PostMapping(value="/uploadAjaxAction", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<ImgDto>> uploadImage(@RequestParam("uploadFile") MultipartFile[] uploadFiles, HttpServletRequest request) {
        String folderPath = path(request);

//        String folderPath = "/Users/jehyeon/Desktop/Team/src/main/webapp/resources/img";

        if(!CheckImg(uploadFiles)){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        String datePath = Todaystr();

        /* 파일 저장 폴더 이름 (오늘 날짜별로 폴더 생성) */
        File uploadPath = new File(folderPath, datePath);

        /* 오늘 날짜 폴더가 있을시 생성 x 없으면 생성 o */
        if(uploadPath.exists() == false) {
            uploadPath.mkdirs();
        }

        List<ImgDto> list = new ArrayList();

        for(MultipartFile multipartFile : uploadFiles) {
            list.add(Makeimg(multipartFile, datePath, uploadPath, "r_",78, 78));
        }

        ResponseEntity<List<ImgDto>> result = new ResponseEntity<>(list, HttpStatus.OK);
        return result;
    }

    //오늘 날짜 구하기
    public String Todaystr(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String str = sdf.format(date);
        return str.replaceAll("-", "_");
    }

    // 이미지 파일체크
    private boolean CheckImg(MultipartFile[] uploadFiles){
        /* 이미지 파일 체크 */
        for(MultipartFile multipartFile: uploadFiles) {

            File checkfile = new File(multipartFile.getOriginalFilename());
            String type = null;

            try {
                type = Files.probeContentType(checkfile.toPath());
//                System.out.println("MIME TYPE : " + type);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(!type.startsWith("image")) {
//                List<ImgDto> list = null;
                return false;
            }
        }
        return true;
    }

    // 타입별 이미지 저장
    //사용 : 판매, 커뮤, 이벤트, 마이페이지, 미리보기용 이미지 생성?
    public ImgDto Makeimg(MultipartFile multipartFile, String datePath, File uploadPath, String imgtype, int width, int height){
        ImgDto imgdto = new ImgDto();
        String uploadFileName = multipartFile.getOriginalFilename();
        // 파일경로 저장
        imgdto.setFilert(datePath);
        //uuid이름 생성
        String uuid = datePath;//UUID.randomUUID().toString();
        imgdto.setU_name(uuid);
        imgdto.setO_name(uploadFileName.substring(0, uploadFileName.indexOf(".")));
        imgdto.setE_name(uploadFileName.substring(uploadFileName.indexOf("."), uploadFileName.length()));

        /* 파일 위치, 파일 이름을 합친 File 객체 */
        File saveFile = new File(uploadPath, uploadFileName);

        /* 파일 저장 */
        try {
            multipartFile.transferTo(saveFile); // 원본이미지 저장

            uploadFileName = uuid + "_" + uploadFileName;

            File thumbnailFile = new File(uploadPath, imgtype + uploadFileName); // 미리보기이미지

            // 이미지 비율 유지하며 크기 조정하여 1:1 비율로 만들기
            BufferedImage Previewimage = Thumbnails.of(multipartFile.getInputStream())
                    .size(width, height)
                    .crop(Positions.CENTER)  // 이미지 중앙을 기준으로 자르기
                    .asBufferedImage();

            Thumbnails.of(Previewimage)
                    .size(width, height)
                    .outputQuality(1.0)  // 품질 유지
                    .toFile(thumbnailFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgdto;
    }

    //본문 이미지 생성
    public void Makeimg2(File imageFile, File uploadPath, String uploadFileName, int width, int height) throws IOException {
        File img_name = new File(uploadPath, uploadFileName);

        BufferedImage image1 = Thumbnails.of(imageFile)
                .size(width, height)
                .crop(Positions.CENTER)  // 이미지 중앙을 기준으로 자르기
                .asBufferedImage();

        Thumbnails.of(image1)
                .size(width, height)
                .outputQuality(1.0)  // 품질 유지
                .toFile(img_name);
    }

    //이미지 저장용도 아직은 하드코딩중
    public void saveDBimg(int tb_no, String tb_name, String imgType, String datePath, ImgDto img, int wsize, int hsize){
        ImgDto imginfo = new ImgDto();
        imginfo.setTb_no(tb_no);
        imginfo.setTb_name(tb_name);
        imginfo.setImgtype(imgType);
        imginfo.setFilert(datePath);
        imginfo.setU_name(imgType+"_"+datePath+"_");
        imginfo.setO_name(img.getO_name());
        imginfo.setE_name(img.getE_name());
        imginfo.setW_size(wsize);
        imginfo.setH_size(hsize);
        imgService.reg_img(imginfo);
    }

    //------디테일 눌렀을때
    @RequestMapping("/testdetail")
    public String testdetail(int no, Model model){
        List<ImgDto> list = imgDao.select_w_imglist(getImglist("sale", "sale_img", "sal_no", no));
        model.addAttribute("list", list);
        return "img/testdetail";
    }

    public HashMap getImglist(String tb_name, String tb_name2, String tb_colum, int no){
        HashMap map = new HashMap<>();
        map.put("tb_name", tb_name);
        map.put("tb_name2", tb_name2);
        map.put("tb_colum", tb_colum);
        map.put("no", no);
        return map;
    }
}
