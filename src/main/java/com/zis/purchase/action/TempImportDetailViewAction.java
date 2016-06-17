package com.zis.purchase.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.BeanUtils;

import com.opensymphony.xwork2.ActionContext;
import com.zis.bookinfo.bean.Bookinfo;
import com.zis.bookinfo.service.BookService;
import com.zis.common.actiontemplate.PaginationQueryAction;
import com.zis.purchase.bean.TempImportDetail;
import com.zis.purchase.bean.TempImportDetailStatus;
import com.zis.purchase.bean.TempImportTask;
import com.zis.purchase.bean.TempImportTaskBizType;
import com.zis.purchase.bean.TempImportTaskStatus;
import com.zis.purchase.biz.DoPurchaseService;
import com.zis.purchase.dto.TempImportDetailView;
import com.zis.purchase.dto.TempImportTaskView;

/**
 * 临时导入数据分页查询Action
 * @author yz
 *
 */
public class TempImportDetailViewAction extends PaginationQueryAction<TempImportDetail> {

	private static final long serialVersionUID = 1L;

	private Integer taskId;
	private String status;

	private BookService bookService;
	private DoPurchaseService doPurchaseService;

	@Override
	protected void doBeforeQuery() {
		this.doPurchaseService.updateTempImportDetail(taskId);
	}
	
	@Override
	protected DetachedCriteria buildQueryCondition() {
		DetachedCriteria criteria = DetachedCriteria.forClass(TempImportDetail.class);
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.add(Restrictions.eq("status", status));
		return criteria;
	}
	
	@Override
	protected void doBeforeReturn() {
		TempImportTask task = this.doPurchaseService.findTempImportTaskByTaskId(taskId);
		// 给下一个页面准备参数
		ActionContext context = ActionContext.getContext();
		TempImportTaskView view = new TempImportTaskView();
		BeanUtils.copyProperties(task, view);
		view.setBizTypeDisplay(TempImportTaskBizType.getBizTypeDisplay(task.getBizType()));
		view.setStatusDisplay(TempImportTaskStatus.getDisplay(task.getStatus()));
		context.put("task", view);
	}

	@Override
	protected List transformResult(List<TempImportDetail> list) {
		// 转换结果
		List<TempImportDetailView> resultList = new ArrayList<TempImportDetailView>();
		for (TempImportDetail detail : list) {
			TempImportDetailView view = new TempImportDetailView();
			BeanUtils.copyProperties(detail, view);
			// 未匹配成功的，查找出可能相关的记录
			if (detail.getStatus().equals(TempImportDetailStatus.NOT_MATCHED)) {
				List<Bookinfo> relatedBooks = this.bookService
						.findBookByISBN(detail.getOrigIsbn());
				view.setRelatedBooks(relatedBooks);
				view.setIsbn(detail.getOrigIsbn());
			}
			// 匹配成功的，查找出匹配的记录
			else if (detail.getStatus().equals(TempImportDetailStatus.MATCHED)) {
				Bookinfo book = this.bookService.findBookById(detail
						.getBookId());
				if (book == null) {
					throw new RuntimeException("图书记录不存在,bookId="
							+ detail.getBookId());
				}
				view.setAssociateBook(book);
			} else {
				throw new RuntimeException("临时导入记录状态不正确, id=" + detail.getId());
			}
			resultList.add(view);
		}
		return resultList;
	}

	@Override
	protected String setActionUrl() {
		if(TempImportDetailStatus.NOT_MATCHED.equals(status)) {
			return "purchase/viewTempImportDetailForNotMatched";
		} else {
			return "purchase/viewTempImportDetailForMatched";
		}
	}
	
	@Override
	protected String setActionUrlQueryCondition() {
		StringBuilder condition = new StringBuilder();
		if (taskId != null) {
			condition.append("taskId=" + taskId + "&");
		}
		if (StringUtils.isNotBlank(status)) {
			condition.append("status=" + status + "&");
		}
		return condition.toString();
	}

	public void setBookService(BookService bookService) {
		this.bookService = bookService;
	}
	
	public void setDoPurchaseService(DoPurchaseService doPurchaseService) {
		this.doPurchaseService = doPurchaseService;
	}

	public Integer getTaskId() {
		return taskId;
	}

	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
