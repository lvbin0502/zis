package com.zis.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.JSON;
import com.opensymphony.xwork2.ActionSupport;
import com.zis.api.response.BaseApiResponse;
import com.zis.api.response.RequiredAmountQueryData;
import com.zis.api.response.RequiredAmountQueryDataV2;
import com.zis.api.response.RequiredAmountQueryResponse;
import com.zis.api.response.RequiredAmountQueryResponseV2;
import com.zis.bookinfo.bean.Bookinfo;
import com.zis.bookinfo.service.BookService;
import com.zis.purchase.bean.PurchasePlan;
import com.zis.purchase.bean.PurchasePlanStatus;
import com.zis.purchase.biz.DoPurchaseService;

public class ApiPurchasedAmountQueryAction extends ActionSupport {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger
			.getLogger(ApiPurchasedAmountQueryAction.class);

	private DoPurchaseService doPurchaseService;
	private BookService bookService;
	private String isbn;

	@Deprecated
	/**
	 * 采购查询接口，已废弃，使用v2
	 * @return
	 */
	public String queryRequiredAmount() {
		logger.info("api.ApiPuchasedAmountQueryAction invoke, isbn=" + isbn);
		RequiredAmountQueryResponse response = new RequiredAmountQueryResponse();
		if (StringUtils.isBlank(isbn)) {
			response.setCode(BaseApiResponse.CODE_ILLEGAL_ARGUMENT);
			response.setMsg("ISNB不能为空!");
			renderResult(response);
			return SUCCESS;
		}
		DetachedCriteria dc = DetachedCriteria.forClass(PurchasePlan.class);
		dc.add(Restrictions.eq("isbn", isbn));
		dc.add(Restrictions.eq("status", PurchasePlanStatus.NORMAL));
		List<PurchasePlan> list = doPurchaseService.findPurchasePlanByCriteria(dc);
		List<RequiredAmountQueryData> resultList = new ArrayList<RequiredAmountQueryData>();
		// 如果采购计划中无此记录，提示系统无此记录
		if (list == null || list.isEmpty()) {
			RequiredAmountQueryData dq = new RequiredAmountQueryData();
			dq.setBookName("无此图书，请联系管理员");
			dq.setBookAuthor("");
			dq.setBookEdition("");
			dq.setBookPublisher("");
			dq.setIsbn(isbn);
			dq.setMemo("无此图书，请联系管理员");
			dq.setRequireAmount(0);
			resultList.add(dq);
		} else {
			for (PurchasePlan plan : list) {
				RequiredAmountQueryData dq = new RequiredAmountQueryData();
				BeanUtils.copyProperties(plan, dq);
				// Integer requireAmount = plan.getRequireAmount()
				// - plan.getPurchasedAmount();
				// if (requireAmount < 0) {
				// requireAmount = 0;
				// }
				dq.setRequireAmount(doPurchaseService.calculateStillRequireAmount(plan));
				resultList.add(dq);
			}
		}
		response.setCode(BaseApiResponse.CODE_SUCCESS);
		response.setResultList(resultList);
		renderResult(response);
		return SUCCESS;
	}
	
	/**
	 * 采购查询接口
	 * @return
	 */
	public String queryRequiredAmountV2() {
		logger.info("api.ApiPuchasedAmountQueryAction invoke, isbn=" + isbn);
		RequiredAmountQueryResponseV2 response = new RequiredAmountQueryResponseV2();
		if (StringUtils.isBlank(isbn)) {
			response.setCode(BaseApiResponse.CODE_ILLEGAL_ARGUMENT);
			response.setMsg("ISNB不能为空!");
			renderResult(response);
			return SUCCESS;
		}
		DetachedCriteria dc = DetachedCriteria.forClass(PurchasePlan.class);
		dc.add(Restrictions.eq("isbn", isbn));
		dc.add(Restrictions.eq("status", PurchasePlanStatus.NORMAL));
		List<PurchasePlan> list = doPurchaseService.findPurchasePlanByCriteria(dc);
		List<RequiredAmountQueryDataV2> resultList = new ArrayList<RequiredAmountQueryDataV2>();
		// 如果采购计划中无此记录，提示系统无此记录
		if (list == null || list.isEmpty()) {
			RequiredAmountQueryDataV2 dq = new RequiredAmountQueryDataV2();
			dq.setBookName("无此图书，请联系管理员");
			dq.setBookAuthor("");
			dq.setBookEdition("");
			dq.setBookPublisher("");
			dq.setIsbn(isbn);
			dq.setMemo("无此图书，请联系管理员");
			dq.setRequireAmount(0);
			resultList.add(dq);
		} else {
			for (PurchasePlan plan : list) {
				RequiredAmountQueryDataV2 dq = new RequiredAmountQueryDataV2();
				BeanUtils.copyProperties(plan, dq);
				Bookinfo book = this.bookService.findBookById(plan.getBookId());
				if(book != null) {
					dq.setNewEdition(book.getIsNewEdition());
				}
				dq.setRequireAmount(doPurchaseService.calculateStillRequireAmount(plan));
				resultList.add(dq);
			}
		}
		response.setCode(BaseApiResponse.CODE_SUCCESS);
		response.setResultList(resultList);
		renderResult(response);
		return SUCCESS;
	}

	private void renderResult(Object obj) {
		// json序列化
//		JSONObject jsonObj = JSONObject.fromObject(obj);
//		String content = jsonObj.toString();
		// FIXME fastjson
		String content = JSON.toJSONString(obj);
		ServletActionContext.getResponse().setContentType(
				"text/html;charset=utf-8");
		try {
			PrintWriter out = ServletActionContext.getResponse().getWriter();
			out.print(content);
			out.flush();
			out.close();
		} catch (IOException e) {
			logger.error("序列化过程失败", e);
		}
	}

	public void setDoPurchaseService(DoPurchaseService doPurchaseService) {
		this.doPurchaseService = doPurchaseService;
	}
	
	public void setBookService(BookService bookService) {
		this.bookService = bookService;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

}