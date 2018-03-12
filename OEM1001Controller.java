/*
 * File Name       : OEM1001Controller.java													<p>
 * Function Number : OEM1001Controller														<p>
 * Module          : 系統管理																	<p>
 * Description     : 系統使用者維護															<p>
 * Author          : 			                    										<p>
 * History         : V1.00 2017/3/8 初始版本													<p>
 * Version         : 1.00																	<p>
 */

package com.taiwanmobile.iot.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiwanmobile.iot.controller.api.BaseAPIController;
import com.taiwanmobile.iot.dto.BaseAPIJsonObject;
import com.taiwanmobile.iot.dto.DataTablesDto;
import com.taiwanmobile.iot.dto.EmailDTO;
import com.taiwanmobile.iot.flfacade.OEM1001Facade;
import com.taiwanmobile.iot.flfacade.common.UniqueSequenceFacade;
import com.taiwanmobile.iot.utility.JsonUtils;
import com.taiwanmobile.iot.utility.LogUtil;
import com.taiwanmobile.iot.utility.LoginUserUtil;
import com.taiwanmobile.iot.utility.ValidateUtil;
import com.taiwanmobile.template.base.BasicForm;
import com.taiwanmobile.template.module.exception.FacadeException;
import com.taiwanmobile.template.module.info.QueryCondition;
import com.taiwanmobile.template.module.page.PageControler;
import com.taiwanmobile.template.module.sort.SortControler;
import com.taiwanmobile.utility.Utility;

@Controller
@RequestMapping(value = "")
public class OEM1001Controller extends BaseAPIController<OEM1001Facade> {

	@Autowired
	private OEM1001Facade OEM1001Facade;
	@Autowired
	private com.taiwanmobile.iot.flfacade.EmailFacade EmailFacade;

	@Autowired
	private UniqueSequenceFacade uniqueSequenceFacade;

	@Autowired
	public OEM1001Controller(OEM1001Facade facade) {
		super(facade);
	}

	private final String[] titleArr = { "ICCID", "MSISDN" };
	private final String[] titleDBArr = { "ICCID", "MSISDN" };
	
	/**
	 * <pre>
	 * Method Name : load
	 * Description : 進入頁
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 */
	@Override
	@RequestMapping(value = "/OEM1001/load")
	public ModelAndView load(HttpServletRequest req, HttpServletResponse res) throws Exception {
		
		try {
			getDscrList(req);
			getAllDscrList(req);
			super.load(req, res);
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			throw e;
		}
		return new ModelAndView("/IOT/OEM100101");
		
	}
	
	/**
	 * <pre>
	 * Method Name : queryAPO
	 * Description : 申裝歷程查詢
	 * </pre>
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return String
	 * @throws Exception
	 */
	@RequestMapping(value = "/OEM1001/queryAPO", method = RequestMethod.POST)
	@ResponseBody
	public String queryAPO(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String orderNo = ValidateUtil.validateHtml(req.getParameter("ORDER_NO"));
		List<BasicForm> list = OEM1001Facade.getAudit(orderNo);
		DataTablesDto<BasicForm> dt = new DataTablesDto<BasicForm>();
		dt.setAaData(list);
		dt.setiTotalDisplayRecords(list.size());
		dt.setiTotalRecords(list.size());
		String toJsonString = toJson(dt);

		return toJsonString;
	}
	
	/**
	 * <pre>
	 * Method Name : export
	 * Description : 匯出頁
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @throws Exception
	 */
	@SuppressWarnings({ "resource", "static-access" })
	@RequestMapping(value = "/OEM1001/export")
	public void export(HttpServletRequest req, HttpServletResponse res ) throws Exception {
		ServletOutputStream out = null;
		
		try {
			//查詢條件
			if(StringUtils.isEmpty(req.getParameter("ORDER_NO"))){
				throw new Exception("匯出失敗");
			}
			List<BasicForm> list = OEM1001Facade.queryByOrderNOList((String) req.getParameter("ORDER_NO"));
			
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet0 = workbook.createSheet("企業申裝異動作業");
			//標題+style
			XSSFFont resultFont = workbook.createFont();
			resultFont.setFontHeight(8);
			resultFont.setBold(true);
			XSSFCellStyle resultStyle = workbook.createCellStyle();
			
			resultStyle.setAlignment(HorizontalAlignment.LEFT);
			resultStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			resultStyle.setFillForegroundColor(IndexedColors.ORANGE.index);
			resultStyle.setFillPattern(resultStyle.getFillPatternEnum().SOLID_FOREGROUND);
			resultStyle.setFont(resultFont);
			//結果標題
			
			int line = 0;
			
			//標頭
			XSSFRow titleRow = sheet0.createRow(line);
			titleRow.setHeightInPoints(12);
			for(int i = 0 ; i < titleArr.length ;i++){
				XSSFCell titleCell = titleRow.createCell(i);
				titleCell.setCellValue(titleArr[i]);
				titleCell.setCellStyle(resultStyle);
			}

			line++;
			
			//內容
			for(int i = 0 ; i < list.size() ; i++){
				BasicForm form = list.get(i);
				XSSFRow resultRow = sheet0.createRow(line);

				for(int j = 0 ; j < titleArr.length ; j++){
					XSSFCell resutCell = resultRow.createCell(j);
					String value = (String)form.getValue(titleDBArr[j]) ;
					resutCell.setCellValue(value);
				}
				line++;
			}
			
			for(int i = 0; i < titleArr.length+2; i++){
				sheet0.autoSizeColumn(i);
			}
			
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String dateString = sdf.format(date);
			String excelName =  dateString+".xlsx";
			
			res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");                      
			res.setCharacterEncoding("UTF-8");             
			res.setHeader("Content-Disposition","attachment;filename="+excelName);
			out = res.getOutputStream();
			workbook.write(out);
			out.flush();
			out.close();
			
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			throw e;
		}finally {
			if(out != null){
				out.close();
			}
		}
	

	}

	/**
	 * <pre>
	 * Method Name : query
	 * Description : 查詢頁
	 * </pre>
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 * 
	 */
	@Override
	@RequestMapping(value = "/OEM1001/query")
	public ModelAndView query(HttpServletRequest req, HttpServletResponse res) throws Exception {

		SortControler sortControler = new SortControler();
		PageControler pageControler = getQueryPageControler();
		QueryCondition queryCondition = getWindowInformation().getQueryCondition();
		// 查詢條件
		if (pageControler.isFirstQuery()) {
			queryCondition.setValue("ORDER_NO", req.getParameter("query(ORDER_NO)"));
			queryCondition.setValue("MSISDN", req.getParameter("query(MSISDN)"));
			queryCondition.setValue("STATUS", req.getParameter("query(STATUS)"));
			queryCondition.setValue("EFF_BOOKING_DATE", req.getParameter("query(EFF_BOOKING_DATE)"));
			queryCondition.setValue("END_BOOKING_DATE", req.getParameter("query(END_BOOKING_DATE)"));
			queryCondition.setValue("ACCT_ID", req.getParameter("query(ACCT_ID)"));
			queryCondition.setValue("ACCT_NAME", req.getParameter("query(ACCT_NAME)"));
			queryCondition.setValue("SALES_BC_ID", req.getParameter("query(SALES_BC_ID)"));
			queryCondition.setValue("SALES_NAME", req.getParameter("query(SALES_NAME)"));

		}
		OEM1001Facade.queryResult(pageControler, sortControler, queryCondition);
		getDscrList(req);
		getAllDscrList(req);
		getRejectTypeList(req);
		return new ModelAndView("/IOT/OEM100101");
	}

	/**
	 * <pre>
	 * Method Name : querySimByAjax
	 * Description : 查詢頁
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 */
	
	@RequestMapping(value = "/OEM1001/querySimByAjax", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String querySimByAjax(HttpServletRequest req,HttpServletResponse res) throws Exception{

		String queryString = req.getParameter("queryInfo") == null ? "{}" : ValidateUtil.validateHtml(req.getParameter("queryInfo"));
		List<BasicForm> lstResult = null;
		JSONObject json = new JSONObject(queryString);

		SortControler sortControler = new SortControler();
		PageControler pageControler = getQueryPageControler();
		QueryCondition queryCondition = getWindowInformation().getQueryCondition();
		// 查詢條件
		if (StringUtils.isEmpty(req.getParameter("ORDER_NO"))) {
			throw new Exception("查詢失敗");
		}
		String ORDER_NO = ValidateUtil.validateHtml(req.getParameter("ORDER_NO"));
		queryCondition.setValue("ORDER_NO", ORDER_NO);
		lstResult = OEM1001Facade.findSimDataByOrderNo(pageControler, sortControler, queryCondition);
		JSONArray jsonArray = JsonUtils.listBasicForm2JsonArray(lstResult);
		json.put("jsonArray", jsonArray);
		json.put("total", pageControler.getTotalRowCount());
		return json.toString();
	}
	/**
	 * <pre>
	 * Method Name : querySim
	 * Description : 查詢頁
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 */
	@RequestMapping(value = "/OEM1001/querySim")
	public ModelAndView querySim(HttpServletRequest req, HttpServletResponse res) throws Exception {
		
		querySim(req);
		getNetWorkTypeList(req);
		getProdItemList(req);
		getSimTypeList(req);
		getPRNWType(req);
		
		return new ModelAndView("/IOT/OEM100102");
	}

	/**
	 * <pre>
	 * Method Name : apply
	 * Description : 進入申請頁
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 *             Exception
	 */

	@RequestMapping(value = "/OEM1001/apply")
	public ModelAndView apply(HttpServletRequest req, HttpServletResponse res) throws Exception {
		try {
		
			getNetWorkTypeList(req);
			getProdItemList(req);
			getSimTypeList(req);
			getMsisdnType(req);
			getPRNWType(req);
			getSimClgrp(req);
			if(req.getParameter("QUERY_ORDER_NO") == null){
				super.load(req, res);
//				getNumber(req);
			}else{
				querySim(req);
			}
			
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			throw e;
		}

		return new ModelAndView("/IOT/OEM100102");
	}
	
	@RequestMapping(value = "/OEM1001/getsimclgrp", method=RequestMethod.POST)
	@ResponseBody
	public String getsimclgrp(HttpServletRequest req, HttpServletResponse res,@RequestBody Map<String, Object> obj) throws Exception {
		List<JSONObject> jsonObject= new ArrayList<JSONObject>();
		BaseAPIJsonObject<JSONObject> resultJson = new BaseAPIJsonObject<JSONObject>();
		try {
			String simClgrp = (String) obj.get("SIMCLGRP");
			List<BasicForm> simClassList = getsimclgrp(simClgrp);
			if(simClassList.size() > 0){
				for(BasicForm form : simClassList){
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("prodItem",(String)form.getValue("DSCR"));
					jsonObj.put("SIM_CLASS",(String)form.getValue("SIM_CLASS"));
					jsonObject.add(jsonObj);
				}
			}
			
			resultJson.setResultList(jsonObject);
			return resultJson.toString();
			
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			return buildSystemErrorJSON(res, e.getLocalizedMessage());
		}
	}
	/**
	 * <pre>
	 * Method Name : saveAddByAjax
	 * Description : 儲存新增
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 *             Exception
	 */
	@RequestMapping(value = "/OEM1001/saveAdd", method = RequestMethod.POST)
	@ResponseBody
	public String saveAddByAjax(HttpServletRequest req, HttpServletResponse res, @RequestBody JSONObject obj) {

		try {
			BasicForm form = new BasicForm();
			form.setResultMap(JsonUtils.toMap(obj));
			// 真正實作
			mLogger.debug("[LOG]OEM1001Controller.add");

			boolean returnObj = OEM1001Facade.saveAdd(form);

			if (returnObj) {
				String resultJson = buildUserProcessSuccessJSON();
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("data", resultJson);
				req.setAttribute("model", model);
				return resultJson;
			} else {
				return buildSystemErrorJSON(res, "送審失敗");
			}
			
		}catch (FacadeException e) {
			LogUtil.printStackTrace(e);
			return buildSystemErrorJSON(res, e.getMessageCode());
		}catch (Exception e) {
			LogUtil.printStackTrace(e);
			return buildSystemErrorJSON(res, e.getMessage());
		}
	}

	/**
	 * <pre>
	 * Method Name : manage
	 * Description : 進入管理頁
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 *             Exception
	 */
	@RequestMapping(value = "/OEM1001/manage")
	public ModelAndView manage(HttpServletRequest req, HttpServletResponse res) throws Exception {
		
		try {
			BasicForm form = OEM1001Facade.queryByOrderNO((String) req.getParameter("ORDER_NO"));
			String repPhoneNbr = Utility.nullToEmpty(form.getValue("REP_PHONE_NBR"));
			if(StringUtils.isNotEmpty(repPhoneNbr) && repPhoneNbr.length() > 7){
				repPhoneNbr = repPhoneNbr.substring(0, 4) + "-" + repPhoneNbr.substring(4, 8);
				form.setValue("REP_PHONE_NBR", repPhoneNbr);
			}
			String prodItem = (String) form.getValue("PROD_ITEM");
			List<BasicForm> simClassList = getSimClass(prodItem);
			
			req.setAttribute("simClassList", simClassList);
			req.setAttribute("data", form);
			
			querySim(req);
			getDscrList(req);
			getAllDscrList(req);
			getRejectTypeList(req);
			getSimTypeList(req);
			getPRNWType(req);
			//add for CR 
			String acctNbr = Utility.nullToEmpty(form.getValue("ACCT_NBR"));
			String billingCycle = Utility.nullToEmpty(form.getValue("BILLING_CYCLE"));
			getBillSendMethod(req);
			getAcctPackageNameDesc(req, acctNbr);
			getValueServiceInfo(req, acctNbr, billingCycle);
			//
			
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			throw e;
		}
		return new ModelAndView("/IOT/OEM100103");
	}



	/**
	 * <pre>
	 * Method Name : anewReview
	 * Description : 重新送審
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 */
	@RequestMapping(value = "/OEM1001/anewReview" , method=RequestMethod.POST)
	@ResponseBody
	public String anewReview(HttpServletRequest req, HttpServletResponse res, @RequestBody JSONObject obj)
			throws Exception {
		try {
			BasicForm form = new BasicForm();
			form.setResultMap(JsonUtils.toMap(obj));

			// 真正實作
			mLogger.debug("[LOG]COM2001Controller.anewReview");
			boolean returnObj = OEM1001Facade.saveAnewReview(form);

			if (returnObj) {
				String resultJson = buildUserProcessSuccessJSON();
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("data", resultJson);
				req.setAttribute("model", model);
				return resultJson;
			} else {
				return buildSystemErrorJSON(res, "核可失敗");
			}

		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			return buildSystemErrorJSON(res, e.getLocalizedMessage());
		}
	}

	/**
	 * <pre>
	 * Method Name : cancle
	 * Description : 取消保留
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response   HttpServletResponse
	 * @return ModelAndView
	 * @throws Exception
	 */
	@RequestMapping(value = "/OEM1001/cancle" , method=RequestMethod.POST)
	@ResponseBody
	public String cancle(HttpServletRequest req, HttpServletResponse res, @RequestBody JSONObject obj)
			throws Exception {
		try {
			BasicForm form = new BasicForm();
			form.setResultMap(JsonUtils.toMap(obj));

			// 真正實作
			mLogger.debug("[LOG]COM2001Controller.cancle");
			boolean returnObj = OEM1001Facade.saveCancle(form);

			if (returnObj) {
				String resultJson = buildUserProcessSuccessJSON();
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("data", resultJson);
				req.setAttribute("model", model);
				return resultJson;
			} else {
				return buildSystemErrorJSON(res, "核可失敗");
			}

		}catch (FacadeException e) {
			LogUtil.printStackTrace(e);
			return buildSystemErrorJSON(res, e.getMessageCode());
		}catch (Exception e) {
			LogUtil.printStackTrace(e);
			return buildSystemErrorJSON(res, e.getMessage());
		}
	}

	/**
	 * <pre>
	 * Method Name : getDscrList
	 * Description : 取得申裝狀態
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getDscrList(HttpServletRequest req) throws Exception {
		List<BasicForm> dscrList = new ArrayList<BasicForm>();
		dscrList = OEM1001Facade.getDscrList();
		req.setAttribute("dscrList", dscrList);
	}
	
	/**
	 * <pre>
	 * Method Name : getMsisdnType
	 * Description : 取得區域碼
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getMsisdnType(HttpServletRequest req) throws Exception {
		List<BasicForm> msisdnTypeList = new ArrayList<BasicForm>();
		msisdnTypeList = OEM1001Facade.getMsisdnType();
		req.setAttribute("msisdnTypeList", msisdnTypeList);
	}
	
	
	/**
	 * <pre>
	 * Method Name : getAllDscrList
	 * Description : 取得申裝狀態
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getAllDscrList(HttpServletRequest req) throws Exception {
		List<BasicForm> dscrList = new ArrayList<BasicForm>();
		dscrList = OEM1001Facade.getAllDscrList();
		req.setAttribute("AllDscrList", dscrList);
	}
	
	/**
	 * <pre>
	 * Method Name : getSimClass
	 * Description : sim卡類別
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	
	public List<BasicForm> getSimClass(String simclgrp) throws Exception {
		 
		List<BasicForm> simClassList  = OEM1001Facade.getSimClass(simclgrp);
		
		return simClassList;
		
	}
	
	
	/**
	 * <pre>
	 * Method Name : getSimClass
	 * Description : sim卡類別
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	
	public List<BasicForm> getsimclgrp(String simclgrp) throws Exception {
		 
		List<BasicForm> simClassList  = OEM1001Facade.getsimclgrp(simclgrp);
		
		return simClassList;
		
	}
	
	/**
	 * <pre>
	 * Method Name : getProdItemList
	 * Description : 取得產品
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getProdItemList(HttpServletRequest req) throws Exception {
		List<BasicForm> prodItemList = new ArrayList<BasicForm>();
		prodItemList = OEM1001Facade.getProdItemList();
		req.setAttribute("prodItemList", prodItemList);
	}

	/**
	 * <pre>
	 * Method Name : getNetWorkTypeList
	 * Description : 取得世代別
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getSimTypeList(HttpServletRequest req) throws Exception {
		List<BasicForm> simTypeList = new ArrayList<BasicForm>();
		simTypeList = OEM1001Facade.getSimTypeList();
		req.setAttribute("simTypeList", simTypeList);
	}
	
	/**
	 * <pre>
	 * Method Name : getNetWorkTypeList
	 * Description : 取得世代別
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getNetWorkTypeList(HttpServletRequest req) throws Exception {
		List<BasicForm> netWorkTypeList = new ArrayList<BasicForm>();
		netWorkTypeList = OEM1001Facade.getNetWorkTypeList();
		req.setAttribute("netWorkTypeList", netWorkTypeList);
	}

	
	/**
	 * <pre>
	 * Method Name : querySim
	 * Description : 取得目前sim卡數量
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response  HttpServletResponse
	 * @return Map<String, Object>
	 * @throws Exception 
	 * 
	 */
	
	
	public void querySim(HttpServletRequest req) throws Exception {
		mLogger.info("SingleTableMaintainAction query");

		SortControler sortControler = new SortControler();
		PageControler pageControler = getQueryPageControler();
		QueryCondition queryCondition = getWindowInformation().getQueryCondition();
		// 查詢條件
		if (pageControler.isFirstQuery()) {
			queryCondition.setValue("PAGE_ORDER_NO", req.getParameter("ORDER_NO"));
			queryCondition.setValue("QUERY_ORDER_NO", req.getParameter("QUERY_ORDER_NO"));
		}

		OEM1001Facade.querySimResult(pageControler, sortControler, queryCondition);
		
	}
	
	/**
	 * <pre>
	 * Method Name : getNumber
	 * Description : 取得連號流水號
	 * </pre>
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	@RequestMapping(value = "/OEM1001/getNumber" , method=RequestMethod.POST)
	@ResponseBody
	public String getNumber(HttpServletRequest req) {
		BaseAPIJsonObject<JSONObject> resultJson = new BaseAPIJsonObject<JSONObject>();
		try {
			String seq = uniqueSequenceFacade.getNextSimBookingOrderNo();
			if(StringUtils.isEmpty(seq)){
				resultJson.setResultCode("");
				resultJson.setResultMessage("取得訂單編號失敗"); 
			}else{
				resultJson.addValueMap("ORDER_NO", seq);
			}
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			resultJson.setResultMessage(e.getMessage()); 
		}
		return resultJson.toString();
	}
	
	/**
	 * <pre>
	 * Method Name : sendMail
	 * Description : 寄信
	 * </pre>
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	@RequestMapping(value = "/OEM1001/sendMail" , method=RequestMethod.POST)
	@ResponseBody
	public String mailTest(HttpServletRequest req,@RequestParam(value = "no") String no) {
		BaseAPIJsonObject<JSONObject> resultJson = new BaseAPIJsonObject<JSONObject>();
		try {
			EmailDTO dto = EmailFacade.getEmailDTO(no, LoginUserUtil.getUser(req));
			EmailFacade.sendEmail(dto);
			mLogger.debug("sendMail:"+no);
		} catch (Exception e) {
			LogUtil.printStackTrace(e);
			resultJson.setResultCode("004");
			resultJson.setResultMessage("寄送EMAIL失敗:"+e.getMessage());
			return resultJson.toString();
		}
		return resultJson.toString();
	}
	
	/**
	 * <pre>
	 * Method Name : getRejectTypeList
	 * Description : 取得申裝狀態
	 * </pre>
	 * 
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return  Map<String, Object>
	 * 
	 */
	public void getRejectTypeList(HttpServletRequest req) throws Exception{
		List<BasicForm> rejectTypeList = new ArrayList<BasicForm>();
		rejectTypeList = OEM1001Facade.getRejectTypeList();
		req.setAttribute("rejectTypeList", rejectTypeList);
	}
	
	/**
	 * <pre>
	 * Method Name : isInteger
	 * Description : 判斷字串是否為數字
	 * </pre>
	 * 
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public static boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
     * 
	 * <pre>
	 * Method Name : toJson
	 * Description : Datatables data object 轉換成 json
	 * </pre>
	 *
	 * @param dt
	 * @return String
     */
    protected String toJson(DataTablesDto<?> dt) {
		ObjectMapper mapper = new ObjectMapper();
		String jSonStr = "";
		try {
			jSonStr = mapper.writeValueAsString(dt);
			
			return jSonStr;
		} catch (JsonProcessingException e) {
			LogUtil.printStackTrace(e);
			return null;
		}
	}
    
    
	/**
	 * <pre>
	 * Method Name : getBillSendMethod
	 * Description : 取得帳單通知方式
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getBillSendMethod(HttpServletRequest req) throws Exception {
		
		String billSendMethod = OEM1001Facade.getBillSendMethod();
		req.setAttribute("billSendMethod", billSendMethod);
	}

	
	/**
	 * <pre>
	 * Method Name : getAcctPackageNameDesc
	 * Description : 取得帳戶資費資訊
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getAcctPackageNameDesc(HttpServletRequest req,String acctNbr) throws Exception {
		
		BasicForm form = OEM1001Facade.getAcctPackageNameDesc(acctNbr);
		req.setAttribute("acctPackageNameDesc", form);
	}

	/**
	 * <pre>
	 * Method Name : getValueServiceInfo
	 * Description : 取得加值服務資訊
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getValueServiceInfo(HttpServletRequest req,String acctNbr,String billingCycle) throws Exception {
		
		String vst = OEM1001Facade.getValueServiceType();
		BasicForm f = OEM1001Facade.getBillingCycleDesc(billingCycle);
		req.setAttribute("valueServiceType", vst);
		req.setAttribute("billingCycle", f);
	}
	
	/** 
	 * <pre>
	 * Method Name : getPRNWType
	 * Description : 取得開通網路
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getPRNWType(HttpServletRequest req) throws Exception {
		
		List<BasicForm> pnt = OEM1001Facade.getPRNWType();
		req.setAttribute("PRNWTypeList", pnt);
	}
	
	/** 
	 * <pre>
	 * Method Name : getSimClgrp
	 * Description : 取得SIM卡類別群組
	 * </pre>
	 * 
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 * 
	 */
	public void getSimClgrp(HttpServletRequest req) throws Exception {
		
		List<BasicForm> simgrp = OEM1001Facade.getSimClgrp();
		req.setAttribute("SimClgrpList", simgrp);
	}
	
}
