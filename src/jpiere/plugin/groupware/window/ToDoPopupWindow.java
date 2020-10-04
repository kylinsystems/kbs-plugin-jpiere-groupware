/******************************************************************************
 * Product: JPiere                                                            *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * JPiere is maintained by OSS ERP Solutions Co., Ltd.                        *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/
package jpiere.plugin.groupware.window;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.ProcessModalDialog;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Mask;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WDatetimeEditor;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.editor.WTimeEditor;
import org.adempiere.webui.editor.WYesNoEditor;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.au.out.AuScript;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.South;
import org.zkoss.zul.Timebox;

import jpiere.plugin.groupware.model.I_ToDo;
import jpiere.plugin.groupware.model.MGroupwareUser;
import jpiere.plugin.groupware.model.MToDo;
import jpiere.plugin.groupware.model.MToDoTeam;
import jpiere.plugin.groupware.util.GroupwareToDoUtil;


/**
 * JPIERE-0473 Personal ToDo Popup Window
 *
 *
 * @author h.hagiwara
 *
 */
public class ToDoPopupWindow extends Window implements EventListener<Event>,ValueChangeListener {

	private static final long serialVersionUID = 7757368164776005797L;

	private static final CLogger log = CLogger.getCLogger(ToDoPopupWindow.class);

	private Properties ctx = null;

	/** Control Parameters	*/
	private boolean p_IsPersonalToDo = true;

	private boolean p_IsUpdatable = false;
	private boolean p_IsUpdatable_ToDoStatus = false;
	private boolean p_IsNewRecord = false;
	private boolean p_haveParentTeamToDo = false;

	private I_ToDo p_iToDo = null;
	private MToDoTeam p_ParentTeamToDo = null;
	private int p_I_ToDo_ID = 0;

	private int p_AD_User_ID = 0;
	private int login_User_ID = 0;

	private String p_JP_ToDo_Type = null;
	private int p_JP_ToDo_Category_ID = 0;
	private Timestamp p_InitialScheduledStartTime = null;
	private Timestamp p_InitialScheduledEndTime = null;
	private boolean p_IsDirty = false;
	private boolean p_Debug = false;

	private int p_Add_Hours = 5;
	private int p_Add_Mins = 15;

	private I_ToDoPopupwindowCaller i_PersonalToDoPopupwindowCaller;

	private List<I_ToDoCalendarEventReceiver>  list_ToDoCalendarEventReceiver = new ArrayList<I_ToDoCalendarEventReceiver>();
	public void addToDoCalenderEventReceiver(I_ToDoCalendarEventReceiver calendar)
	{
		list_ToDoCalendarEventReceiver.add(calendar);
	}


	private  List<I_ToDo>  list_ToDoes = null;
	private int index = 0;


	/*** Web Components ***/
	// WEditors & Labels
	private Map<String, Label> map_Label = new HashMap<String, Label>();
	private Map<String, WEditor> map_Editor = new HashMap<String, WEditor>();

	//Layout
	private North north = null;
	private Center center = null;
	private ConfirmPanel confirmPanel;

	//Buttons
	private Button zoomPersonalToDoBtn = null;
	private Button zoomTeamToDoBtn = null;
	private Button undoBtn = null;
	private Button saveBtn = null;
	private Button processBtn = null;
	private Button leftBtn = null;
	private Button rightBtn = null;
	private Button deleteBtn = null;

	private Button addStartHoursBtn = null;
	private Button addStartMinsBtn = null;
	private Button addEndHoursBtn = null;
	private Button addEndMinsBtn = null;

	/** Process PopupWindow Components **/
	private Popup popup = null;

	//*** Constants ***//
	private final static String BUTTON_NAME_ZOOM_PERSONALTODO = "ZOOM_P";
	private final static String BUTTON_NAME_ZOOM_TEAMTODO = "ZOOM_T";
	private final static String BUTTON_NAME_UNDO = "REDO";
	private final static String BUTTON_NAME_SAVE = "SAVE";
	private final static String BUTTON_NAME_PROCESS = "PROCESS";
	private final static String BUTTON_KICK_PROCESS = "KICK_PROCESS";

	private final static String BUTTON_NAME_PREVIOUS_TODO = "PREVIOUS";
	private final static String BUTTON_NAME_NEXT_TODO = "NEXT";
	private final static String BUTTON_NAME_DELETE = "DELETE";
	private final static String BUTTON_NAME_ADD_START_HOURS = "ADD_START_HOURS";
	private final static String BUTTON_NAME_ADD_START_MINS = "ADD_START_MINS";
	private final static String BUTTON_NAME_ADD_END_HOURS = "ADD_END_HOURS";
	private final static String BUTTON_NAME_ADD_END_MINS = "ADD_END_MINS";



	private boolean isKickedProcess = false;

	/**
	 * Constructor
	 */
	public ToDoPopupWindow(I_ToDoPopupwindowCaller caller, int index)
	{
		super();

		this.i_PersonalToDoPopupwindowCaller = caller;

		this.list_ToDoes =caller.getToDoList();
		this.index = index;
		ctx = Env.getCtx();
		login_User_ID = Env.getAD_User_ID(ctx);

		setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);
		if (!ThemeManager.isUseCSSForWindowSize()) {
			ZKUpdateUtil.setWindowWidthX(this, 400);
			ZKUpdateUtil.setWindowHeightX(this, 600);
		} else {
			addCallback(AFTER_PAGE_ATTACHED, t -> {
				ZKUpdateUtil.setCSSHeight(this);
				ZKUpdateUtil.setCSSWidth(this);
			});
		}

		this.setSclass("popup-dialog request-dialog");
		this.setBorder("normal");
		this.setShadow(true);
		this.setClosable(true);

		if(index <= -1)
		{
			p_I_ToDo_ID = 0;
			p_IsNewRecord = true;

			String JP_ToDo_Calendar = i_PersonalToDoPopupwindowCaller.getJP_ToDo_Calendar();
			if(MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo.equals(JP_ToDo_Calendar))
			{
				p_IsPersonalToDo = true;

			}else {

				p_IsPersonalToDo = false;
			}

		}else {

			p_iToDo = list_ToDoes.get(index);
			if(p_iToDo instanceof MToDo)
			{
				p_IsPersonalToDo = true;

			}else {
				p_IsPersonalToDo = false;
			}


			p_I_ToDo_ID = list_ToDoes.get(index).get_ID();
			p_IsNewRecord = false;
		}

		MGroupwareUser gUser = MGroupwareUser.get(ctx, login_User_ID);
		if(gUser != null)
		{
			p_Add_Hours = gUser.getJP_Add_Hours();
			p_Add_Mins = gUser.getJP_Add_Mins();
		}

		updateControlParameter(p_I_ToDo_ID);
		updateWindowTitle();

		createLabelMap();
		createEditorMap();
		updateEditorStatus();
		updateEditorValue();

		Borderlayout borderlayout = new Borderlayout();
		this.appendChild(borderlayout);
		ZKUpdateUtil.setHflex(borderlayout, "1");
		ZKUpdateUtil.setVflex(borderlayout, "1");

		north = new North();
		north = updateNorth();
		borderlayout.appendChild(north);

		center = new Center();
		center.setSclass("dialog-content");
		center.setAutoscroll(true);
		center = updateCenter();
		borderlayout.appendChild(center);

		if(p_IsNewRecord)
		{
			South southPane = new South();
			southPane.setSclass("dialog-footer");
			borderlayout.appendChild(southPane);
			confirmPanel = new ConfirmPanel(true);
			confirmPanel.addActionListener(this);
			southPane.appendChild(confirmPanel);
		}
	}

	private void updateControlParameter(int JP_ToDo_ID)
	{
		p_I_ToDo_ID = JP_ToDo_ID;

		if(p_I_ToDo_ID == 0)
		{
			p_iToDo = null;
			p_AD_User_ID = i_PersonalToDoPopupwindowCaller.getDefault_AD__User_ID();
			p_JP_ToDo_Type = i_PersonalToDoPopupwindowCaller.getDefault_JP_ToDo_Type();
			p_JP_ToDo_Category_ID = i_PersonalToDoPopupwindowCaller.getDefault_JP_ToDo_Category_ID ();

		}else {

			p_AD_User_ID = p_iToDo.getAD_User_ID();
			p_JP_ToDo_Type = p_iToDo.getJP_ToDo_Type();
			p_JP_ToDo_Category_ID = p_iToDo.getJP_ToDo_Category_ID();
		}

		if(p_IsNewRecord)
		{
			p_IsUpdatable = true;

		}else {

			if(p_AD_User_ID == login_User_ID || p_iToDo.getCreatedBy() == login_User_ID)
			{
				if(p_iToDo.isProcessed())
					p_IsUpdatable = false;
				else
					p_IsUpdatable = true;

			}else {

				p_IsUpdatable = false;
			}
		}

		if(p_iToDo == null)
		{
			p_IsUpdatable_ToDoStatus = true;
		}else if(p_AD_User_ID == login_User_ID || p_iToDo.getCreatedBy() == login_User_ID){
			p_IsUpdatable_ToDoStatus = true;
		}else {
			p_IsUpdatable_ToDoStatus = false;
		}

		if(p_IsNewRecord)
		{
			p_haveParentTeamToDo = false;
			p_ParentTeamToDo = null;
		}else if(p_iToDo.getParent_Team_ToDo_ID() == 0) {
			p_haveParentTeamToDo = false;
			p_ParentTeamToDo = null;
		}else {
			p_haveParentTeamToDo = true;
			p_ParentTeamToDo = new MToDoTeam(ctx, p_iToDo.getParent_Team_ToDo_ID(), null);
		}


		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
		{
			p_InitialScheduledStartTime = i_PersonalToDoPopupwindowCaller.getDefault_JP_ToDo_ScheduledStartTime();
			p_InitialScheduledEndTime = i_PersonalToDoPopupwindowCaller.getDefault_JP_ToDo_ScheduledEndTime();

		}else if(MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type)){

			p_InitialScheduledStartTime = null;
			p_InitialScheduledEndTime = i_PersonalToDoPopupwindowCaller.getDefault_JP_ToDo_ScheduledStartTime();

		}else {

			p_InitialScheduledStartTime = null;
			p_InitialScheduledEndTime = null;

		}
	}

	private void createLabelMap()
	{
		map_Label.put(MToDo.COLUMNNAME_AD_Org_ID, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_AD_Org_ID)) );
		map_Label.put(MToDo.COLUMNNAME_AD_User_ID, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_AD_User_ID)) );
		map_Label.get(MToDo.COLUMNNAME_AD_User_ID).setStyle("font-weight:bold;border-left: 4px solid #F39700;padding-left:2px;");
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_Type, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Type)) );
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_Category_ID, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Category_ID)) );
		map_Label.put(MToDo.COLUMNNAME_Name, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_Name)) );
		map_Label.put(MToDo.COLUMNNAME_Description, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_Description)) );
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate)) );
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime)) );
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate)) );
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime)) );
		map_Label.put(MToDo.COLUMNNAME_JP_ToDo_Status, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Status)) );
		map_Label.put(MToDo.COLUMNNAME_IsOpenToDoJP, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_IsOpenToDoJP)) );

		if(p_IsPersonalToDo)
		{
			map_Label.put(MToDo.COLUMNNAME_Comments, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_Comments)) );
			map_Label.put(MToDo.COLUMNNAME_JP_Statistics_YesNo, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_YesNo)));
			map_Label.put(MToDo.COLUMNNAME_JP_Statistics_Choice, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_Choice)));
			map_Label.put(MToDo.COLUMNNAME_JP_Statistics_DateAndTime, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_DateAndTime)));
			map_Label.put(MToDo.COLUMNNAME_JP_Statistics_Number, new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_Number)));

		}else {
			map_Label.put(MToDoTeam.COLUMNNAME_JP_Team_ID, new Label(Msg.getElement(ctx, MToDoTeam.COLUMNNAME_JP_Team_ID)) );
			map_Label.put(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info, new Label(Msg.getElement(ctx, MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info)) );
		}
	}

	private void createEditorMap()
	{
		//*** AD_Org_ID ***//
		MLookup lookup_AD_Org_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_AD_Org_ID),  DisplayType.Search);
		WSearchEditor Editor_AD_Org_ID = new WSearchEditor(lookup_AD_Org_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_AD_Org_ID), null, true, p_IsNewRecord? false : true, true);
		Editor_AD_Org_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(Editor_AD_Org_ID.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_AD_Org_ID, Editor_AD_Org_ID);


		//*** AD_User_ID ***//
		MLookup lookup_AD_User_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_AD_User_ID),  DisplayType.Search);
		WSearchEditor Editor_AD_User_ID = new WSearchEditor(lookup_AD_User_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_AD_User_ID), null, true, p_IsNewRecord? false : true, true);
		Editor_AD_User_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(Editor_AD_User_ID.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_AD_User_ID, Editor_AD_User_ID);


		//*** JP_ToDo_Type ***//
		MLookup lookup_JP_ToDo_Type = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name,  MToDo.COLUMNNAME_JP_ToDo_Type),  DisplayType.List);
		//WTableDirEditor editor_JP_ToDo_Type = new WTableDirEditor(lookup_JP_ToDo_Type, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Type), null, true, p_IsTeamToDo? true : !p_IsUpdatable, true);
		WTableDirEditor editor_JP_ToDo_Type = new WTableDirEditor(MToDo.COLUMNNAME_JP_ToDo_Type, true, p_haveParentTeamToDo? true : !p_IsUpdatable, true, lookup_JP_ToDo_Type);
		editor_JP_ToDo_Type.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Type.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Type, editor_JP_ToDo_Type);


		//*** JP_ToDo_Category_ID ***//
		String validationCode = null;
		if(p_IsPersonalToDo)
			validationCode = "JP_ToDo_Category.AD_User_ID IS NULL OR JP_ToDo_Category.AD_User_ID=" + p_AD_User_ID;
		else
			validationCode = "JP_ToDo_Category.AD_User_ID IS NULL ";
		MLookup lookup_JP_ToDo_Category_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Category_ID),  DisplayType.Search);
		lookup_JP_ToDo_Category_ID.getLookupInfo().ValidationCode = validationCode;
		WSearchEditor editor_JP_ToDo_Category_ID = new WSearchEditor(lookup_JP_ToDo_Category_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Category_ID), null, false, p_haveParentTeamToDo? true : !p_IsUpdatable, true);
		editor_JP_ToDo_Category_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Category_ID.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Category_ID, editor_JP_ToDo_Category_ID);


		//*** JP_Team_ID ***//
		if(!p_IsPersonalToDo)
		{
			validationCode = "JP_Team.AD_User_ID IS NULL OR JP_Team.AD_User_ID=" + Env.getAD_User_ID(ctx);//Login user
			MLookup lookup_JP_Team_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDoTeam.Table_Name, MToDoTeam.COLUMNNAME_JP_Team_ID),  DisplayType.Search);
			lookup_JP_Team_ID.getLookupInfo().ValidationCode = validationCode;
			WSearchEditor editor_JP_Team_ID = new WSearchEditor(lookup_JP_Team_ID, Msg.getElement(ctx, MToDoTeam.COLUMNNAME_JP_Team_ID), null, false, !p_IsUpdatable, true);
			editor_JP_Team_ID.addValueChangeListener(this);
			ZKUpdateUtil.setHflex(editor_JP_Team_ID.getComponent(), "true");
			map_Editor.put(MToDoTeam.COLUMNNAME_JP_Team_ID, editor_JP_Team_ID);
		}


		//*** Name ***//
		WStringEditor editor_Name = new WStringEditor(MToDo.COLUMNNAME_Name, true, p_haveParentTeamToDo? true : !p_IsUpdatable, true, 30, 30, "", null);
		editor_Name.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_Name.getComponent(), "true");
		editor_Name.getComponent().setRows(2);
		map_Editor.put(MToDo.COLUMNNAME_Name, editor_Name);


		//*** Description ***//
		WStringEditor editor_Description = new WStringEditor(MToDo.COLUMNNAME_Description, true, p_haveParentTeamToDo? true : !p_IsUpdatable, true, 30, 30, "", null);
		editor_Description.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_Description.getComponent(), "true");
		editor_Description.getComponent().setRows(3);
		map_Editor.put(MToDo.COLUMNNAME_Description, editor_Description);


		//*** Comments ***//
		if(p_IsPersonalToDo)
		{
			WStringEditor editor_Comments = new WStringEditor(MToDo.COLUMNNAME_Comments, true, !p_IsUpdatable, true, 30, 30, "", null);
			editor_Comments.addValueChangeListener(this);
			ZKUpdateUtil.setHflex(editor_Comments.getComponent(), "true");
			editor_Comments.getComponent().setRows(3);
			map_Editor.put(MToDo.COLUMNNAME_Comments, editor_Comments);
		}

		//*** JP_ToDo_ScheduledStartDate ***//
		WDateEditor editor_JP_ToDo_ScheduledStartDate = new WDateEditor(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate, false, p_haveParentTeamToDo? true : !p_IsUpdatable, true, null);
		editor_JP_ToDo_ScheduledStartDate.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_ScheduledStartDate.getComponent(), "true");
		map_Editor.put("JP_ToDo_ScheduledStartDate", editor_JP_ToDo_ScheduledStartDate);

		//*** IsStarDateAllDayJP ***//
		WYesNoEditor editor_IsStartDateAllDayJP = new WYesNoEditor(MToDo.COLUMNNAME_IsStartDateAllDayJP, Msg.getElement(ctx, MToDo.COLUMNNAME_IsStartDateAllDayJP), null, true, !p_IsUpdatable, true);
		editor_IsStartDateAllDayJP.addValueChangeListener(this);
		map_Editor.put(MToDo.COLUMNNAME_IsStartDateAllDayJP, editor_IsStartDateAllDayJP);

		//*** JP_ToDo_ScheduledStartTime ***//
		WTimeEditor editor_JP_ToDo_ScheduledStartTime = new WTimeEditor(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime, false, p_haveParentTeamToDo? true : !p_IsUpdatable, true, null);
		editor_JP_ToDo_ScheduledStartTime.addValueChangeListener(this);
		//ZKUpdateUtil.setWidth(editor_JP_ToDo_ScheduledStartTime.getComponent(), "80px");
		ZKUpdateUtil.setHflex(editor_JP_ToDo_ScheduledStartTime.getComponent(), "true");
		Timebox startTimebox = editor_JP_ToDo_ScheduledStartTime.getComponent();
		startTimebox.setFormat("HH:mm");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime, editor_JP_ToDo_ScheduledStartTime);

		//*** JP_ToDo_ScheduledEndDate ***//
		WDateEditor editor_JP_ToDo_ScheduledEndDate = new WDateEditor(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate, false, p_haveParentTeamToDo? true :!p_IsUpdatable, true, null);
		editor_JP_ToDo_ScheduledEndDate.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_ScheduledEndDate.getComponent(), "true");
		map_Editor.put("JP_ToDo_ScheduledEndDate", editor_JP_ToDo_ScheduledEndDate);

		//*** IsEndDateAllDayJP ***//
		WYesNoEditor editor_IsEndDateAllDayJP = new WYesNoEditor(MToDo.COLUMNNAME_IsEndDateAllDayJP, Msg.getElement(ctx, MToDo.COLUMNNAME_IsEndDateAllDayJP), null, true, !p_IsUpdatable, true);
		editor_IsEndDateAllDayJP.addValueChangeListener(this);
		map_Editor.put(MToDo.COLUMNNAME_IsEndDateAllDayJP, editor_IsEndDateAllDayJP);

		//*** JP_ToDo_ScheduledEndTime ***//
		WTimeEditor editor_JP_ToDo_ScheduledEndTime = new WTimeEditor(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime, false, p_haveParentTeamToDo? true :!p_IsUpdatable, true, null);
		editor_JP_ToDo_ScheduledEndTime.addValueChangeListener(this);
		//ZKUpdateUtil.setWidth(editor_JP_ToDo_ScheduledEndTime.getComponent(), "80px");
		ZKUpdateUtil.setHflex(editor_JP_ToDo_ScheduledEndTime.getComponent(), "true");
		Timebox endTimebox = editor_JP_ToDo_ScheduledEndTime.getComponent();
		endTimebox.setFormat("HH:mm");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime, editor_JP_ToDo_ScheduledEndTime);

		//*** JP_ToDo_Status ***//
		MLookup lookup_JP_ToDo_Status = MLookupFactory.get(ctx, 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Status),  DisplayType.List);
		WTableDirEditor editor_JP_ToDo_Status = new WTableDirEditor(lookup_JP_ToDo_Status, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Status), null, true, p_IsUpdatable_ToDoStatus? false: true, true);
		editor_JP_ToDo_Status.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Status.getComponent(), "true");
		map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Status, editor_JP_ToDo_Status);

		//*** IsOpenToDoJP ***//
		WYesNoEditor editor_IsOpenToDoJP = new WYesNoEditor(MToDo.COLUMNNAME_IsOpenToDoJP, Msg.getElement(ctx, MToDo.COLUMNNAME_IsOpenToDoJP), null, true, !p_IsUpdatable, true);
		editor_IsOpenToDoJP.addValueChangeListener(this);
		map_Editor.put(MToDo.COLUMNNAME_IsOpenToDoJP, editor_IsOpenToDoJP);


		//*** Statistics Info ***/
		if(p_IsPersonalToDo)
		{
			//*** JP_Statistics_YesNo  ***//
			MLookup lookup_JP_Statistics_YesNo = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_Statistics_YesNo),  DisplayType.List);
			WTableDirEditor editor_JP_Statistics_YesNo = new WTableDirEditor(lookup_JP_Statistics_YesNo, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_YesNo), null, false, !p_IsUpdatable, true);
			editor_JP_Statistics_YesNo.addValueChangeListener(this);
			ZKUpdateUtil.setHflex(editor_JP_Statistics_YesNo.getComponent(), "true");
			map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_YesNo, editor_JP_Statistics_YesNo);

			//*** JP_Statistics_Choice ***//
			MLookup lookup_JP_Statistics_Choice = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_Statistics_Choice),  DisplayType.List);
			WTableDirEditor editor_JP_Statistics_Choice = new WTableDirEditor(lookup_JP_Statistics_Choice, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_Choice), null, false, !p_IsUpdatable, true);
			editor_JP_Statistics_Choice.addValueChangeListener(this);
			ZKUpdateUtil.setHflex(editor_JP_Statistics_Choice.getComponent(), "true");
			map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_Choice, editor_JP_Statistics_Choice);

			//*** JP_Statistics_DateAndTime ***//
			WDatetimeEditor editor_JP_Statistics_DateAndTime = new WDatetimeEditor(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_DateAndTime), null, false, !p_IsUpdatable, true);
			editor_JP_Statistics_DateAndTime.addValueChangeListener(this);
			ZKUpdateUtil.setHflex((HtmlBasedComponent)editor_JP_Statistics_DateAndTime.getComponent(), "true");
			map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_DateAndTime, editor_JP_Statistics_DateAndTime);

			//*** JP_Statistics_Number ***//
			WNumberEditor editor_JP_Statistics_Number = new WNumberEditor(MToDo.COLUMNNAME_JP_Statistics_Number, false, !p_IsUpdatable, true, DisplayType.Number, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_Statistics_Number));
			editor_JP_Statistics_Number.addValueChangeListener(this);
			ZKUpdateUtil.setHflex(editor_JP_Statistics_Number.getComponent(), "true");
			map_Editor.put(MToDo.COLUMNNAME_JP_Statistics_Number, editor_JP_Statistics_Number);

		}else {

			//*** JP_Mandatory_Statistics_Info ***//
			MLookup lookup_JP_Mandatory_Statistics_Info = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDoTeam.Table_Name,  MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info),  DisplayType.List);
			WTableDirEditor editor_JP_Mandatory_Statistics_Info= new WTableDirEditor(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info, true, !p_IsUpdatable, true, lookup_JP_Mandatory_Statistics_Info);
			editor_JP_Mandatory_Statistics_Info.addValueChangeListener(this);
			ZKUpdateUtil.setHflex(editor_JP_Mandatory_Statistics_Info.getComponent(), "true");
			map_Editor.put(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info, editor_JP_Mandatory_Statistics_Info);

		}
	}

	private void updateEditorStatus()
	{
		map_Editor.get(MToDo.COLUMNNAME_AD_Org_ID).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_Name).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_Description).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_IsStartDateAllDayJP).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_IsEndDateAllDayJP).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).setReadWrite(p_IsUpdatable_ToDoStatus? true: false);
		map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).setReadWrite(p_haveParentTeamToDo? false : p_IsUpdatable);

		if(p_IsPersonalToDo)
		{
			map_Editor.get(MToDo.COLUMNNAME_Comments).setReadWrite(p_IsUpdatable);
			map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo).setReadWrite(p_IsUpdatable);
			map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice).setReadWrite(p_IsUpdatable);
			map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime).setReadWrite(p_IsUpdatable);
			map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number).setReadWrite(p_IsUpdatable);

		}else {
			map_Editor.get(MToDoTeam.COLUMNNAME_JP_Team_ID).setReadWrite(p_IsUpdatable);
			map_Editor.get(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info).setReadWrite(p_IsUpdatable);
		}

		if(addStartHoursBtn != null)
		{
			addStartHoursBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addStartMinsBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addEndHoursBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addEndMinsBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
		}

	}

	private void updateEditorValue()
	{
		if(p_IsNewRecord)
		{
			map_Editor.get(MToDo.COLUMNNAME_AD_Org_ID).setValue(0);
			map_Editor.get(MToDo.COLUMNNAME_AD_User_ID).setValue(p_AD_User_ID);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).setValue(p_JP_ToDo_Type);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).setValue(p_JP_ToDo_Category_ID);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate).setValue(p_InitialScheduledStartTime);
			map_Editor.get(MToDo.COLUMNNAME_IsStartDateAllDayJP).setValue("N");
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setValue(p_InitialScheduledStartTime);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate).setValue(p_InitialScheduledEndTime);
			map_Editor.get(MToDo.COLUMNNAME_IsEndDateAllDayJP).setValue("N");
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).setValue(p_InitialScheduledEndTime);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).setValue(MToDo.JP_TODO_STATUS_NotYetStarted);
			map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).setValue("Y");

		}else {

			map_Editor.get(MToDo.COLUMNNAME_AD_Org_ID).setValue(p_iToDo.getAD_Org_ID());
			map_Editor.get(MToDo.COLUMNNAME_AD_User_ID).setValue(p_AD_User_ID);
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).setValue(p_iToDo.getJP_ToDo_Type());
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).setValue(p_iToDo.getJP_ToDo_Category_ID());
			map_Editor.get(MToDo.COLUMNNAME_Name).setValue(p_iToDo.getName());
			map_Editor.get(MToDo.COLUMNNAME_Description).setValue(p_iToDo.getDescription());
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate).setValue(p_iToDo.getJP_ToDo_ScheduledStartTime());
			map_Editor.get(MToDo.COLUMNNAME_IsStartDateAllDayJP).setValue(p_iToDo.isStartDateAllDayJP());
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setValue(p_iToDo.getJP_ToDo_ScheduledStartTime());
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate).setValue(p_iToDo.getJP_ToDo_ScheduledEndTime());
			map_Editor.get(MToDo.COLUMNNAME_IsEndDateAllDayJP).setValue(p_iToDo.isEndDateAllDayJP());
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).setValue(p_iToDo.getJP_ToDo_ScheduledEndTime());
			map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).setValue(p_iToDo.getJP_ToDo_Status());
			map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).setValue(p_iToDo.isOpenToDoJP());

			if(p_IsPersonalToDo)
			{
				map_Editor.get(MToDo.COLUMNNAME_Comments).setValue(p_iToDo.getComments());
				map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo).setValue(p_iToDo.getJP_Statistics_YesNo());
				map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice).setValue(p_iToDo.getJP_Statistics_Choice());
				map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime).setValue(p_iToDo.getJP_Statistics_DateAndTime());
				map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number).setValue(p_iToDo.getJP_Statistics_Number());
			}else {
				map_Editor.get(MToDoTeam.COLUMNNAME_JP_Team_ID).setValue(p_iToDo.getJP_Team_ID());
				map_Editor.get(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info).setValue(p_iToDo.getJP_Mandatory_Statistics_Info());
			}

		}
	}


	private void updateWindowTitle()
	{
		if(p_IsNewRecord)
		{
			if(p_IsPersonalToDo)
				setTitle("[" + Msg.getElement(ctx,MToDo.COLUMNNAME_JP_ToDo_ID) + "] " + Msg.getMsg(ctx, "NewRecord"));
			else
				setTitle("[" + Msg.getElement(ctx,MToDoTeam.COLUMNNAME_JP_ToDo_Team_ID) + "] " + Msg.getMsg(ctx, "NewRecord"));

		}else {

			String name = GroupwareToDoUtil.trimName(MUser.getNameOfUser(p_iToDo.getCreatedBy()));
			if(p_IsPersonalToDo)
			{
				if(p_iToDo.getParent_Team_ToDo_ID() == 0)
				{
					setTitle("[" + Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ID) + "] "
							+ Msg.getElement(Env.getCtx(),MToDo.COLUMNNAME_CreatedBy)
							+ ":" + name);

				}else {

					setTitle("[" + Msg.getElement(ctx,MToDo.COLUMNNAME_JP_ToDo_Team_ID) + "] "
							+ Msg.getElement(Env.getCtx(),MToDo.COLUMNNAME_CreatedBy)
							+ ":" + name);
				}

			}else {

				setTitle("[" + Msg.getElement(ctx,MToDo.COLUMNNAME_JP_ToDo_Team_ID) + "] "
						+ Msg.getElement(Env.getCtx(),MToDo.COLUMNNAME_CreatedBy)
						+ ":" + name);

			}

		}

	}

	private North updateNorth()
	{
		if(north.getFirstChild() != null)
			north.getFirstChild().detach();

		if(p_IsNewRecord)
			return north;

		Hlayout hlyaout = new Hlayout();
		hlyaout.setStyle("margin:2px 2px 2px 2px; padding:2px 2px 2px 2px;");// border: solid 1px #dddddd;
		north.appendChild(hlyaout);


		//Personal ToDo Zoom Button
		if(p_IsPersonalToDo)
		{
			if(zoomPersonalToDoBtn == null)
			{
				zoomPersonalToDoBtn = new Button();
				zoomPersonalToDoBtn.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
				zoomPersonalToDoBtn.setClass("btn-small");
				zoomPersonalToDoBtn.setName(BUTTON_NAME_ZOOM_PERSONALTODO);
				zoomPersonalToDoBtn.setTooltiptext(Msg.getMsg(ctx, "JP_Zoom_To_PersonalToDo"));
				zoomPersonalToDoBtn.addEventListener(Events.ON_CLICK, this);
			}
			hlyaout.appendChild(zoomPersonalToDoBtn);
		}


		//Team ToDo Zoom Button
		if(zoomTeamToDoBtn == null)
		{
			zoomTeamToDoBtn = new Button();
			zoomTeamToDoBtn.setImage(ThemeManager.getThemeResource("images/ZoomAcross16.png"));
			zoomTeamToDoBtn.setClass("btn-small");
			zoomTeamToDoBtn.setName(BUTTON_NAME_ZOOM_TEAMTODO);
			zoomTeamToDoBtn.setTooltiptext(Msg.getMsg(ctx, "JP_Zoom_To_TeamToDo"));
			zoomTeamToDoBtn.addEventListener(Events.ON_CLICK, this);
		}

		if(p_IsPersonalToDo)
		{
			if(p_ParentTeamToDo == null)
			{
				zoomTeamToDoBtn.setEnabled(false);
			}else {
				zoomTeamToDoBtn.setEnabled(true);
			}
		}else {
			zoomTeamToDoBtn.setEnabled(true);
		}
		hlyaout.appendChild(zoomTeamToDoBtn);


		hlyaout.appendChild(GroupwareToDoUtil.getDividingLine());


		//Undo Button
		if(undoBtn == null)
		{
			undoBtn = new Button();
			undoBtn.setImage(ThemeManager.getThemeResource("images/Undo16.png"));
			undoBtn.setClass("btn-small");
			undoBtn.setName(BUTTON_NAME_UNDO);
			undoBtn.setTooltiptext(Msg.getMsg(ctx, "Ignore"));
			undoBtn.addEventListener(Events.ON_CLICK, this);
		}
		if(p_IsDirty)
			undoBtn.setEnabled(true);
		else
			undoBtn.setEnabled(false);
		hlyaout.appendChild(undoBtn);


		//Save Button
		if(saveBtn == null)
		{
			saveBtn = new Button();
			saveBtn.setImage(ThemeManager.getThemeResource("images/Save16.png"));
			saveBtn.setClass("btn-small");
			saveBtn.setName(BUTTON_NAME_SAVE);
			saveBtn.setTooltiptext(Msg.getMsg(ctx, "Save"));
			saveBtn.addEventListener(Events.ON_CLICK, this);
		}
		if(p_IsDirty)
			saveBtn.setEnabled(true);
		else
			saveBtn.setEnabled(false);
		hlyaout.appendChild(saveBtn);


		//Process Button
		if(processBtn == null)
		{
			processBtn = new Button();
			processBtn.setImage(ThemeManager.getThemeResource("images/Process16.png"));
			processBtn.setClass("btn-small");
			processBtn.setName(BUTTON_NAME_PROCESS);
			processBtn.setTooltiptext(Msg.getMsg(ctx, "Process"));
			processBtn.addEventListener(Events.ON_CLICK, this);
		}
		hlyaout.appendChild(processBtn);

		hlyaout.appendChild(GroupwareToDoUtil.getDividingLine());


		//Left Button
		if(leftBtn  == null)
		{
			leftBtn = new Button();
			leftBtn.setImage(ThemeManager.getThemeResource("images/MoveLeft16.png"));
			leftBtn.setClass("btn-small");
			leftBtn.setName(BUTTON_NAME_PREVIOUS_TODO);
			leftBtn.setTooltiptext(Msg.getMsg(ctx, "Previous"));
			leftBtn.addEventListener(Events.ON_CLICK, this);
		}
		hlyaout.appendChild(leftBtn);
		if(index == 0)
			leftBtn.setEnabled(false);
		else
			leftBtn.setEnabled(true);


		//Right Button
		if(rightBtn == null)
		{
			rightBtn = new Button();
			rightBtn.setImage(ThemeManager.getThemeResource("images/MoveRight16.png"));
			rightBtn.setClass("btn-small");
			rightBtn.setName(BUTTON_NAME_NEXT_TODO);
			rightBtn.setTooltiptext(Msg.getMsg(ctx, "Next"));
			rightBtn.addEventListener(Events.ON_CLICK, this);
		}
		hlyaout.appendChild(rightBtn);
		if(index == list_ToDoes.size()-1)
			rightBtn.setEnabled(false);
		else
			rightBtn.setEnabled(true);


		StringBuilder msg = new StringBuilder(p_IsDirty? "*" : "");
		msg = msg.append((index + 1) + " / " + list_ToDoes.size());

		if(p_Debug)
		{
			msg = msg.append(" | " + Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_ID) +  ":" + p_iToDo.get_ID());

			if(p_haveParentTeamToDo)
			{
				msg = msg.append(" | " + Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Team_ID) +  ":" + p_iToDo.getParent_Team_ToDo_ID());
			}
		}


		hlyaout.appendChild(GroupwareToDoUtil.createLabelDiv(null, msg.toString(),true));
		hlyaout.appendChild(GroupwareToDoUtil.getDividingLine());


		//Delete Button
		if(deleteBtn == null)
		{
			deleteBtn = new Button();
			deleteBtn.setImage(ThemeManager.getThemeResource("images/Delete16.png"));
			deleteBtn.setClass("btn-small");
			deleteBtn.setName(BUTTON_NAME_DELETE);
			deleteBtn.setTooltiptext(Msg.getMsg(ctx, "Delete"));
			deleteBtn.addEventListener(Events.ON_CLICK, this);
		}
		deleteBtn.setEnabled(p_IsUpdatable);
		hlyaout.appendChild(deleteBtn);

		return north;

	}

	private Center updateCenter()
	{
		if(center.getFirstChild() != null)
			center.getFirstChild().detach();

		if(addStartHoursBtn == null)
		{
			addStartHoursBtn = new Button();
			addStartHoursBtn.setClass("btn-small");
			addStartHoursBtn.setName(BUTTON_NAME_ADD_START_HOURS);
			addStartHoursBtn.setLabel("+"+p_Add_Hours+Msg.getMsg(ctx, "JP_Hours"));
			addStartHoursBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addStartHoursBtn.addEventListener(Events.ON_CLICK, this);
			ZKUpdateUtil.setHflex(addStartHoursBtn, "true");
		}

		if(addStartMinsBtn == null)
		{
			addStartMinsBtn = new Button();
			addStartMinsBtn.setClass("btn-small");
			addStartMinsBtn.setName(BUTTON_NAME_ADD_START_MINS);
			addStartMinsBtn.setLabel("+"+p_Add_Mins+Msg.getMsg(ctx, "JP_Mins"));
			addStartMinsBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addStartMinsBtn.addEventListener(Events.ON_CLICK, this);
			ZKUpdateUtil.setHflex(addStartMinsBtn, "true");
		}

		if(addEndHoursBtn == null)
		{
			addEndHoursBtn = new Button();
			addEndHoursBtn.setClass("btn-small");
			addEndHoursBtn.setName(BUTTON_NAME_ADD_END_HOURS);
			addEndHoursBtn.setLabel("+"+p_Add_Hours+Msg.getMsg(ctx, "JP_Hours"));
			addEndHoursBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addEndHoursBtn.addEventListener(Events.ON_CLICK, this);
			ZKUpdateUtil.setHflex(addEndHoursBtn, "true");
		}

		if(addEndMinsBtn == null)
		{
			addEndMinsBtn = new Button();
			addEndMinsBtn.setClass("btn-small");
			addEndMinsBtn.setName(BUTTON_NAME_ADD_END_MINS);
			addEndMinsBtn.setLabel("+"+p_Add_Mins+Msg.getMsg(ctx, "JP_Mins"));
			addEndMinsBtn.setVisible(p_haveParentTeamToDo? false : p_IsUpdatable);
			addEndMinsBtn.addEventListener(Events.ON_CLICK, this);
			ZKUpdateUtil.setHflex(addEndMinsBtn, "true");
		}

		Div centerContent = new Div();
		center.appendChild(centerContent);
		ZKUpdateUtil.setVflex(center, "min");

		Grid grid = GridFactory.newGridLayout();
		ZKUpdateUtil.setVflex(grid, "min");
		ZKUpdateUtil.setHflex(grid, "1");
		centerContent.appendChild(grid);

		Rows rows = grid.newRows();

		//*** AD_Org_ID ***//
		Row row = rows.newRow();
		rows.appendChild(row);
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_AD_Org_ID), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_AD_Org_ID).getComponent(),4);


		//*** AD_User_ID ***//
		row = rows.newRow();
		rows.appendChild(row);
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_AD_User_ID), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_AD_User_ID).getComponent(),4);


		//*** JP_ToDo_Type ***//
		row = rows.newRow();
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_Type), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type).getComponent(),4);


		//*** JP_ToDo_Category_ID ***//
		row = rows.newRow();
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID), false),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).getComponent(),4);
		map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID).showMenu();


		//*** Name ***//
		row = rows.newRow();
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_Name), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_Name).getComponent(),4);


		//*** Description ***//
		row = rows.newRow();
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_Description), false),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_Description).getComponent(),4);


		//*** Comments ***//
		if(p_IsPersonalToDo)
		{
			row = rows.newRow();
			row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_Comments), false),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_Comments).getComponent(),4);
		}

		if(!p_IsPersonalToDo)
		{
			row = rows.newRow();
			row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDoTeam.COLUMNNAME_JP_Team_ID), false),2);
			row.appendCellChild(map_Editor.get(MToDoTeam.COLUMNNAME_JP_Team_ID).getComponent(),4);
		}


		//*** JP_ToDo_ScheduledStartDate & Time ***//
		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
		{
			row = rows.newRow();
			row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate), true),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate).getComponent(),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_IsStartDateAllDayJP).getComponent(),2);

			WEditor editor = map_Editor.get(MToDo.COLUMNNAME_IsStartDateAllDayJP);
			boolean IsStartDateAllDayJP = (boolean)editor.getValue();
			if(IsStartDateAllDayJP)
			{
				;//Noting to do
			}else {

				row = rows.newRow();
				row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime), true),2);
				Timebox comp = (Timebox)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).getComponent();
				if(p_IsUpdatable && !p_haveParentTeamToDo)
					comp.setButtonVisible(true);
				else
					comp.setButtonVisible(false);

				row.appendCellChild(comp,2);
				row.appendCellChild(addStartHoursBtn,1);
				row.appendCellChild(addStartMinsBtn,1);
			}
		}

		//*** JP_ToDo_ScheduledEndDate & Time ***//
		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type) || MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type) )
		{
			row = rows.newRow();
			row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate), true),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate).getComponent(),2);
			row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_IsEndDateAllDayJP).getComponent(),2);

			WEditor editor = map_Editor.get(MToDo.COLUMNNAME_IsEndDateAllDayJP);
			boolean IsEndDateAllDayJP = (boolean)editor.getValue();
			if(IsEndDateAllDayJP)
			{
				;//Noting to do
			}else {

				row = rows.newRow();
				row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime), true),2);
				Timebox comp = (Timebox)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime).getComponent();
				if(p_IsUpdatable && !p_haveParentTeamToDo)
					comp.setButtonVisible(true);
				else
					comp.setButtonVisible(false);

				row.appendCellChild(comp,2);
				row.appendCellChild(addEndHoursBtn,1);
				row.appendCellChild(addEndMinsBtn,1);
			}
		}

		//*** JP_ToDo_Status ***//
		row = rows.newRow();
		row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_ToDo_Status), true),2);
		row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status).getComponent(),2);


		//*** IsOpenToDoJP ***//
		Div div_IsOpenToDoJP = new Div();
		div_IsOpenToDoJP.appendChild(map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP).getComponent());
		row.appendCellChild(div_IsOpenToDoJP,2);


		/********************************************************************************************
		 * Statistics Info
		 ********************************************************************************************/
		if(p_IsPersonalToDo && p_I_ToDo_ID == 0)
		{
			return center;

		}else if(p_IsPersonalToDo && p_iToDo.getParent_Team_ToDo_ID() == 0) {

			return center;

		}else if(p_IsPersonalToDo && p_iToDo.getParent_Team_ToDo_ID() != 0) {

			if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_None.equals(p_ParentTeamToDo.getJP_Mandatory_Statistics_Info()))
			{
				return center;
			}
		}


		row = rows.newRow();
		Groupbox statisticsInfo_GroupBox = new Groupbox();
		statisticsInfo_GroupBox.setOpen(true);
		row.appendCellChild(statisticsInfo_GroupBox,6);

		String caption = Msg.getMsg(Env.getCtx(),"JP_StatisticsInfo");
		statisticsInfo_GroupBox.appendChild(new Caption(caption));
		Grid statisticsInfo_Grid  = GridFactory.newGridLayout();
		statisticsInfo_Grid.setStyle("background-color: #E9F0FF");
		statisticsInfo_Grid.setStyle("border: none");
		statisticsInfo_GroupBox.appendChild(statisticsInfo_Grid);

		Rows statisticsInfo_rows = statisticsInfo_Grid.newRows();

		if(p_IsPersonalToDo)
		{
			String JP_Mandatory_Statistics_Info = null;
			if(p_ParentTeamToDo!=null)
			{
				JP_Mandatory_Statistics_Info = p_ParentTeamToDo.getJP_Mandatory_Statistics_Info();
			}

			//*** JP_Statistics_YesNo  ***//
			if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_YesNo.equals(JP_Mandatory_Statistics_Info))
			{
				row = statisticsInfo_rows.newRow();
				row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_YesNo), true),2);
				row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo).getComponent(),4);
			}

			//*** JP_Statistics_Choice ***//
			if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Choice.equals(JP_Mandatory_Statistics_Info))
			{
				row = statisticsInfo_rows.newRow();
				row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Choice), true),2);
				row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice).getComponent(),4);
			}

			//*** JP_Statistics_DateAndTime ***//
			if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_DateAndTime.equals(JP_Mandatory_Statistics_Info))
			{
				row = statisticsInfo_rows.newRow();
				row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime), true),2);
				row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime).getComponent(),4);
			}

			//*** JP_Statistics_Number ***//
			if(MToDoTeam.JP_MANDATORY_STATISTICS_INFO_Number.equals(JP_Mandatory_Statistics_Info))
			{
				row = statisticsInfo_rows.newRow();
				row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDo.COLUMNNAME_JP_Statistics_Number), true),2);
				row.appendCellChild(map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number).getComponent(),4);
			}

		}else {

			row = statisticsInfo_rows.newRow();
			row.appendCellChild(GroupwareToDoUtil.createLabelDiv(map_Label.get(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info), false),2);
			row.appendCellChild(map_Editor.get(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info).getComponent(),4);
		}

		return center;
	}



	public void onEvent(Event event) throws Exception
	{
		Component comp = event.getTarget();

		if (p_IsNewRecord && event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_OK))
		{

			if(saveToDo())
			{
				this.detach();
			}


		}
		else if (p_IsNewRecord && event.getTarget() == confirmPanel.getButton(ConfirmPanel.A_CANCEL))
		{
			this.detach();

		}else if(comp instanceof Button) {

			Button btn = (Button) comp;
			String btnName = btn.getName();
			if(BUTTON_NAME_PREVIOUS_TODO.equals(btnName))
			{
				if(p_IsDirty)
					saveToDo();

				index--;
				if(index >= 0 )
				{
					p_IsDirty = false;
					p_iToDo = list_ToDoes.get(index);
					p_I_ToDo_ID = p_iToDo.get_ID();
					updateControlParameter(list_ToDoes.get(index).get_ID());
					updateWindowTitle();
					updateEditorValue();
					updateEditorStatus();
					updateNorth();
					updateCenter();

				}else {
					index = 0;
					updateNorth();
				}

			}else if(BUTTON_NAME_NEXT_TODO.equals(btnName)){

				if(p_IsDirty)
					saveToDo();

				index++;
				if(index < list_ToDoes.size())
				{
					p_IsDirty = false;
					p_iToDo = list_ToDoes.get(index);
					p_I_ToDo_ID = p_iToDo.get_ID();
					updateControlParameter(list_ToDoes.get(index).get_ID());
					updateWindowTitle();
					updateEditorValue();
					updateEditorStatus();
					updateNorth();
					updateCenter();

				}else {
					index = list_ToDoes.size()-1;
					updateNorth();
				}

			}else if(BUTTON_NAME_ZOOM_PERSONALTODO.equals(btnName)){

				AEnv.zoom(MTable.getTable_ID(MToDo.Table_Name), p_I_ToDo_ID);
				this.detach();

			}else if(BUTTON_NAME_ZOOM_TEAMTODO.equals(btnName)){

				if(p_IsPersonalToDo)
					AEnv.zoom(MTable.getTable_ID(MToDoTeam.Table_Name), p_ParentTeamToDo.getJP_ToDo_Team_ID());
				else
					AEnv.zoom(MTable.getTable_ID(MToDoTeam.Table_Name), p_iToDo.get_ID());
				this.detach();

			}else if(BUTTON_NAME_SAVE.equals(btnName)){

				saveToDo();

			}else if(BUTTON_NAME_PROCESS.equals(btnName)){

				if(p_IsDirty)
				{
					if(!saveToDo())
					{
						return ;
					}
				}

				createProcessPopupWindow();

				return;

			}else if(BUTTON_KICK_PROCESS.equals(btnName)) {

				int AD_Process_ID =(Integer)btn.getAttribute("AD_Process_ID");
				ProcessInfo pi = new ProcessInfo("", AD_Process_ID, MTable.getTable_ID(MToDo.Table_Name), p_iToDo.get_ID());
				ProcessModalDialog dialog = new ProcessModalDialog(this, i_PersonalToDoPopupwindowCaller.getWindowNo(), pi, true);

				dialog.setBorder("normal");

				this.appendChild(dialog);

				if (ClientInfo.isMobile())
				{
					dialog.doHighlighted();
				}
				else
				{
					showBusyMask(this);
					LayoutUtils.openOverlappedWindow(this, dialog, "middle_center");
					dialog.focus();
				}

				isKickedProcess = true;
				popup.close();

			}else if(BUTTON_NAME_DELETE.equals(btnName)){

				deleteToDo();

			}else if(BUTTON_NAME_UNDO.equals(btnName)) {

				p_IsDirty = false;
				//updateControlParameter(list_ToDoes.get(index).getJP_ToDo_ID());
				//updateWindowTitle();
				updateEditorValue();
				//updateEditorStatus();
				updateNorth();
				updateCenter();

			}else if(BUTTON_NAME_ADD_START_HOURS.equals(btnName) || BUTTON_NAME_ADD_START_MINS.equals(btnName)) {

				p_IsDirty = true;
				updateNorth();

				WDateEditor scheduledStartDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
				Timestamp ts_ScheduledStartDate =(Timestamp)scheduledStartDate.getValue();
				if(ts_ScheduledStartDate == null)
				{
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
					throw new WrongValueException(scheduledStartDate.getComponent(), msg);
				}

				WTimeEditor scheduledStartTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
				Timestamp ts_ScheduledStartTime =(Timestamp)scheduledStartTime.getValue();
				LocalTime local_StartTime = null;
				if(ts_ScheduledStartTime == null)
				{
					local_StartTime = LocalTime.MIN;
				}else {
					local_StartTime = ts_ScheduledStartTime.toLocalDateTime().toLocalTime();
					if(BUTTON_NAME_ADD_START_HOURS.equals(btnName))
					{
						local_StartTime = local_StartTime.plusHours(p_Add_Hours);
					}else if(BUTTON_NAME_ADD_START_MINS.equals(btnName)) {
						local_StartTime = local_StartTime.plusMinutes(p_Add_Mins);
					}
				}
				ts_ScheduledStartTime = Timestamp.valueOf(LocalDateTime.of(ts_ScheduledStartDate.toLocalDateTime().toLocalDate(), local_StartTime));
				scheduledStartDate.setValue(ts_ScheduledStartTime);
				scheduledStartTime.setValue(ts_ScheduledStartTime);
				scheduledStartTime.getComponent().focus();

				WDateEditor scheduledEndDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
				Timestamp ts_ScheduledEndDate =(Timestamp)scheduledEndDate.getValue();
				if(ts_ScheduledEndDate != null)
				{
					if(ts_ScheduledStartTime.compareTo(ts_ScheduledEndDate) >= 0 )
					{
						WTimeEditor scheduledEndTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
						scheduledEndDate.setValue(ts_ScheduledStartTime);
						scheduledEndTime.setValue(ts_ScheduledStartTime);
					}
				}

			}else if(BUTTON_NAME_ADD_END_HOURS.equals(btnName) || BUTTON_NAME_ADD_END_MINS.equals(btnName) ) {//TODO

				p_IsDirty = true;
				updateNorth();

				WDateEditor scheduledEndDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
				Timestamp ts_ScheduledEndDate =(Timestamp)scheduledEndDate.getValue();
				if(ts_ScheduledEndDate == null)
				{
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
					throw new WrongValueException(scheduledEndDate.getComponent(), msg);
				}

				WTimeEditor scheduledEndTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
				Timestamp ts_ScheduledEndTime =(Timestamp)scheduledEndTime.getValue();
				LocalTime local_EndTime = null;
				if(ts_ScheduledEndTime == null)
				{
					local_EndTime = LocalTime.MIN;
				}else {
					local_EndTime = ts_ScheduledEndTime.toLocalDateTime().toLocalTime();
					if(BUTTON_NAME_ADD_END_HOURS.equals(btnName))
					{
						local_EndTime = local_EndTime.plusHours(p_Add_Hours);
					}else if(BUTTON_NAME_ADD_END_MINS.equals(btnName)) {
						local_EndTime = local_EndTime.plusMinutes(p_Add_Mins);
					}
				}
				ts_ScheduledEndTime = Timestamp.valueOf(LocalDateTime.of(ts_ScheduledEndDate.toLocalDateTime().toLocalDate(), local_EndTime));
				scheduledEndDate.setValue(ts_ScheduledEndTime);
				scheduledEndTime.setValue(ts_ScheduledEndTime);
				scheduledEndTime.getComponent().focus();

				if(MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type))
				{
					map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate).setValue(ts_ScheduledEndTime);
					map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setValue(ts_ScheduledEndTime);

				}else if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type)) {

					WDateEditor scheduledStartDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
					Timestamp ts_ScheduledStartDate =(Timestamp)scheduledStartDate.getValue();
					if(ts_ScheduledStartDate != null)
					{
						if(ts_ScheduledStartDate.compareTo(ts_ScheduledEndTime) >= 0 )
						{
							WTimeEditor scheduledStartTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
							scheduledStartDate.setValue(ts_ScheduledEndTime);
							scheduledStartTime.setValue(ts_ScheduledEndTime);
						}

					}

				}
			}

		}
		else if (event.getTarget() instanceof ProcessModalDialog)
    	{
    		if (!DialogEvents.ON_WINDOW_CLOSE.equals(event.getName())){
    			return;
    		}

    		hideBusyMask();
    		ProcessModalDialog dialog = (ProcessModalDialog) event.getTarget();
    		ProcessInfo pi = dialog.getProcessInfo();
			MPInstance instance = new MPInstance(ctx, pi.getAD_PInstance_ID(), "false");
			String msg= instance.getErrorMsg();
			if(instance.getResult() == 0 && !Util.isEmpty(msg))
				FDialog.error(i_PersonalToDoPopupwindowCaller.getWindowNo(), this, msg);
			else if(instance.getResult() == 0 && Util.isEmpty(msg))
				;
			else
				FDialog.info(i_PersonalToDoPopupwindowCaller.getWindowNo(), this, msg);
    	}
	}

	private Mask mask = null;
	private Div getMask()
	{
		if (mask == null) {
			mask = new Mask();
		}
		return mask;
	}

	public void hideBusyMask()
	{
		if (mask != null && mask.getParent() != null) {
			mask.detach();
			StringBuilder script = new StringBuilder("var w=zk.Widget.$('#");
			script.append(getUuid()).append("');if(w) w.busy=false;");
			Clients.response(new AuScript(script.toString()));
		}
	}

	public void showBusyMask(Window window)
	{

		appendChild(getMask());
		StringBuilder script = new StringBuilder("var w=zk.Widget.$('#");
		script.append(getUuid()).append("');");
		if (window != null) {
			script.append("var d=zk.Widget.$('#").append(window.getUuid()).append("');w.busy=d;");
		} else {
			script.append("w.busy=true;");
		}
		Clients.response(new AuScript(script.toString()));
	}

	private boolean saveToDo()
	{
		if(!p_IsUpdatable && !p_IsUpdatable_ToDoStatus)
		{
			return true;
		}

		if(p_IsNewRecord)
		{
			if(p_IsPersonalToDo)
			{
				p_iToDo = new MToDo(Env.getCtx(), 0, null);
			}else {
				p_iToDo = new MToDoTeam(Env.getCtx(), 0, null);
			}
		}

		//TODO
		Timestamp old_ScheduledStartTime = p_iToDo.getJP_ToDo_ScheduledStartTime();
		Timestamp new_ScheduledStartTime = null;

		Timestamp old_ScheduledEndTime = p_iToDo.getJP_ToDo_ScheduledEndTime();
		Timestamp new_ScheduledEndTime = null;

		//Check AD_Org_ID
		WEditor editor = map_Editor.get(MToDo.COLUMNNAME_AD_Org_ID);
		if(editor.getValue() == null)
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_AD_Org_ID);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_iToDo.setAD_Org_ID((Integer)editor.getValue());
		}

		//Check AD_User_ID
		editor = map_Editor.get(MToDo.COLUMNNAME_AD_User_ID);
		if(editor.getValue() == null || ((Integer)editor.getValue()).intValue() == 0)
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_AD_User_ID);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_iToDo.setAD_User_ID((Integer)editor.getValue());
		}

		//Check JP_ToDo_Type
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Type);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_Type);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_iToDo.setJP_ToDo_Type((String)editor.getValue());
		}

		//Check JP_ToDo_Category_ID
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Category_ID);
		if(editor.getValue() == null || ((Integer)editor.getValue()).intValue() == 0)
		{
			;
		}else {
			p_iToDo.setJP_ToDo_Category_ID((Integer)editor.getValue());
		}

		//Check Name
		editor = map_Editor.get(MToDo.COLUMNNAME_Name);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_Name);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_iToDo.setName((String)editor.getValue());
		}

		//Check Description
		editor = map_Editor.get(MToDo.COLUMNNAME_Description);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			p_iToDo.setDescription(null);
		}else {
			p_iToDo.setDescription(editor.getValue().toString());
		}

		//Check Comments
		if(p_IsPersonalToDo)
		{
			editor = map_Editor.get(MToDo.COLUMNNAME_Comments);
			if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
			{
				p_iToDo.setComments(null);
			}else {
				p_iToDo.setComments(editor.getValue().toString());
			}

		}

		//Check JP_Team_ID
		if(!p_IsPersonalToDo)
		{
			editor = map_Editor.get(MToDoTeam.COLUMNNAME_JP_Team_ID);
			if(editor.getValue() == null || ((Integer)editor.getValue()).intValue() == 0)
			{
				p_iToDo.setJP_Team_ID(0);
			}else {
				p_iToDo.setJP_Team_ID((Integer)editor.getValue());
			}
		}


		//Check JP_ToDo_ScheduledStartDate & Time
		LocalDate date = null;
		LocalTime time = null;
		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
		{
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
			if(editor.getValue() == null)
			{
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
				throw new WrongValueException(editor.getComponent(), msg);

			}else {
				date = ((Timestamp)editor.getValue()).toLocalDateTime().toLocalDate();
				p_iToDo.setJP_ToDo_ScheduledStartDate((Timestamp)editor.getValue());
			}

			//Set IsStartDateAllDayJP
			editor = map_Editor.get(MToDo.COLUMNNAME_IsStartDateAllDayJP);
			p_iToDo.setIsStartDateAllDayJP(((boolean)editor.getValue()));

			//Check JP_ToDo_ScheduledStartTime
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
			if(!p_iToDo.isStartDateAllDayJP() && editor.getValue() == null)
			{
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
				throw new WrongValueException(editor.getComponent(), msg);

			}else {

				if(p_iToDo.isStartDateAllDayJP())
				{
					p_iToDo.setJP_ToDo_ScheduledStartTime(Timestamp.valueOf(LocalDateTime.of(date, LocalTime.MIN)));
				}else{
					time = ((Timestamp)editor.getValue()).toLocalDateTime().toLocalTime();
					p_iToDo.setJP_ToDo_ScheduledStartTime(Timestamp.valueOf(LocalDateTime.of(date, time)));
				}
				p_iToDo.setJP_ToDo_ScheduledStartDate(p_iToDo.getJP_ToDo_ScheduledStartTime());
			}
			new_ScheduledStartTime = p_iToDo.getJP_ToDo_ScheduledStartTime();
		}


		//Check JP_ToDo_ScheduledEndDate & Time
		if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type) || MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type))
		{
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
			if(editor.getValue() == null)
			{
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
				throw new WrongValueException(editor.getComponent(), msg);

			}else {
				date = ((Timestamp)editor.getValue()).toLocalDateTime().toLocalDate();
				p_iToDo.setJP_ToDo_ScheduledEndDate((Timestamp)editor.getValue());
			}

			//Set IsEndDateAllDayJP
			editor = map_Editor.get(MToDo.COLUMNNAME_IsEndDateAllDayJP);
			p_iToDo.setIsEndDateAllDayJP(((boolean)editor.getValue()));

			//Check JP_ToDo_ScheduledEndTime
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
			if(!p_iToDo.isEndDateAllDayJP() && editor.getValue() == null)
			{
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
				throw new WrongValueException(editor.getComponent(), msg);

			}else {

				if(p_iToDo.isEndDateAllDayJP())
				{
					p_iToDo.setJP_ToDo_ScheduledEndTime(Timestamp.valueOf(LocalDateTime.of(date, LocalTime.MIN)));
				}else {
					time = ((Timestamp)editor.getValue()).toLocalDateTime().toLocalTime();
					p_iToDo.setJP_ToDo_ScheduledEndTime(Timestamp.valueOf(LocalDateTime.of(date, time)));
				}
				p_iToDo.setJP_ToDo_ScheduledEndDate(p_iToDo.getJP_ToDo_ScheduledEndTime());
			}

			new_ScheduledEndTime = p_iToDo.getJP_ToDo_ScheduledEndTime();

			if(MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type))
			{
				p_iToDo.setJP_ToDo_ScheduledStartDate(p_iToDo.getJP_ToDo_ScheduledEndDate());
				p_iToDo.setJP_ToDo_ScheduledStartTime(p_iToDo.getJP_ToDo_ScheduledEndTime());
				new_ScheduledStartTime = p_iToDo.getJP_ToDo_ScheduledEndTime();
			}


		}

		if(MToDo.JP_TODO_TYPE_Memo.equals(p_JP_ToDo_Type))
		{
			p_iToDo.setJP_ToDo_ScheduledStartDate(null);
			p_iToDo.setJP_ToDo_ScheduledStartTime(null);
			p_iToDo.setJP_ToDo_ScheduledEndDate(null);
			p_iToDo.setJP_ToDo_ScheduledEndTime(null);
		}


		//Check JP_ToDo_Status
		editor = map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_Status);
		if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
		{
			String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_Status);
			throw new WrongValueException(editor.getComponent(), msg);

		}else {
			p_iToDo.setJP_ToDo_Status(editor.getValue().toString());
		}


		//Set IsOpenToDoJP
		editor = map_Editor.get(MToDo.COLUMNNAME_IsOpenToDoJP);
		p_iToDo.setIsOpenToDoJP(((boolean)editor.getValue()));

		if(p_IsPersonalToDo)
		{
			//Set JP_Statistics_YesNo
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_YesNo);
			p_iToDo.setJP_Statistics_YesNo(((String)editor.getValue()));

			//Set JP_Statistics_Choice
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Choice);
			p_iToDo.setJP_Statistics_Choice(((String)editor.getValue()));

			//Set JP_Statistics_DateAndTime
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_DateAndTime);
			p_iToDo.setJP_Statistics_DateAndTime(((Timestamp)editor.getValue()));

			//Set JP_Statistics_Number
			editor = map_Editor.get(MToDo.COLUMNNAME_JP_Statistics_Number);
			p_iToDo.setJP_Statistics_Number(((BigDecimal)editor.getValue()));

		}else {

			//Set JP_Mandatory_Statistics_Info
			editor = map_Editor.get(MToDoTeam.COLUMNNAME_JP_Mandatory_Statistics_Info);
			if(editor.getValue() == null || Util.isEmpty(editor.getValue().toString()))
			{
				;

			}else {
				p_iToDo.setJP_Mandatory_Statistics_Info(editor.getValue().toString());
			}

		}

		String msg = p_iToDo.beforeSavePreCheck(true);
		if(!Util.isEmpty(msg))
		{
			FDialog.error(0, this, msg);
			return false;
		}

		if (p_iToDo.save())
		{
			if (log.isLoggable(Level.FINE)) log.fine("JP_ToDo_ID=" + p_iToDo.get_ID());

			p_IsDirty = false;

			updateControlParameter(p_iToDo.get_ID());
			updateEditorValue();
			updateEditorStatus();
			updateNorth();
			updateCenter();

			if(p_IsNewRecord)
			{
				for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
				{
					receiveToDoCalendarEvent.create(p_iToDo);
				}

			}else {

				for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
				{
					receiveToDoCalendarEvent.update(p_iToDo);
				}
			}


		}
		else
		{
			FDialog.error(0, this, Msg.getMsg(ctx, "SaveError") + " : "+ Msg.getMsg(ctx, "JP_UnexpectedError"));
			return false;
		}


		//Update Related ToDo
		if(!p_IsNewRecord)//TODO
		{
			if(new_ScheduledStartTime.compareTo(old_ScheduledStartTime) != 0
					||	new_ScheduledEndTime.compareTo(old_ScheduledEndTime) != 0)
			{
				if(p_iToDo instanceof MToDo)
				{
					MToDo m_ToDo =	(MToDo)p_iToDo;
					ArrayList<MToDo> list = MToDo.getRelatedToDos(ctx, m_ToDo, null, old_ScheduledStartTime, true, null);

					if(list.size() > 0)
					{
						long between_ScheduledStartMins = ChronoUnit.MINUTES.between(old_ScheduledStartTime.toLocalDateTime(), new_ScheduledStartTime.toLocalDateTime());
						long between_ScheduledEndMins = ChronoUnit.MINUTES.between(old_ScheduledEndTime.toLocalDateTime(), new_ScheduledEndTime.toLocalDateTime());

						Callback<Boolean> isRelaredToDoUpdate = new Callback<Boolean>()
						{
								@Override
								public void onCallback(Boolean result)
								{
									if(result)
									{
										Timestamp scheduledStartTime = null;
										Timestamp scheduledEndTime = null;

										for(MToDo todo : list)
										{
											if(m_ToDo.getJP_ToDo_ID() == todo.getJP_ToDo_ID())
												continue;

											scheduledStartTime = Timestamp.valueOf(todo.getJP_ToDo_ScheduledStartTime().toLocalDateTime().plusMinutes(between_ScheduledStartMins));
											scheduledEndTime = Timestamp.valueOf(todo.getJP_ToDo_ScheduledEndTime().toLocalDateTime().plusMinutes(between_ScheduledEndMins));

											todo.setJP_ToDo_ScheduledStartDate(scheduledStartTime);
											todo.setJP_ToDo_ScheduledStartTime(scheduledStartTime);

											todo.setJP_ToDo_ScheduledEndDate(scheduledEndTime);
											todo.setJP_ToDo_ScheduledEndTime(scheduledEndTime);
											if(!todo.save())
											{
												//TODO エラー処理
											}
										}

										for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
										{
											receiveToDoCalendarEvent.refresh(p_iToDo);
										}
									}
								}
						};
						FDialog.ask(i_PersonalToDoPopupwindowCaller.getWindowNo(), null,"JP_ToDo_Update_CreatedRepeatedly1", Msg.getMsg(ctx, "JP_ToDo_Update_CreatedRepeatedly2"), isRelaredToDoUpdate);
					}


				}else if(p_iToDo instanceof MToDoTeam) {

					MToDoTeam m_TeamToDo =	(MToDoTeam)p_iToDo;
					ArrayList<MToDoTeam> list = MToDoTeam.getRelatedTeamToDos(ctx, m_TeamToDo, null, old_ScheduledStartTime, true, null);

					if(list.size() > 0)
					{

						long between_ScheduledStartMins = ChronoUnit.MINUTES.between(old_ScheduledStartTime.toLocalDateTime(), new_ScheduledStartTime.toLocalDateTime());
						long between_ScheduledEndMins = ChronoUnit.MINUTES.between(old_ScheduledEndTime.toLocalDateTime(), new_ScheduledEndTime.toLocalDateTime());

						Callback<Boolean> isRelaredToDoUpdate = new Callback<Boolean>()
						{
								@Override
								public void onCallback(Boolean result)
								{
									if(result)
									{
										Timestamp scheduledStartTime = null;
										Timestamp scheduledEndTime = null;

										for(MToDoTeam todo : list)
										{
											if(m_TeamToDo.getJP_ToDo_Team_ID() == todo.getJP_ToDo_Team_ID())
												continue;

											scheduledStartTime = Timestamp.valueOf(todo.getJP_ToDo_ScheduledStartTime().toLocalDateTime().plusMinutes(between_ScheduledStartMins));
											scheduledEndTime = Timestamp.valueOf(todo.getJP_ToDo_ScheduledEndTime().toLocalDateTime().plusMinutes(between_ScheduledEndMins));

											todo.setJP_ToDo_ScheduledStartDate(scheduledStartTime);
											todo.setJP_ToDo_ScheduledStartTime(scheduledStartTime);

											todo.setJP_ToDo_ScheduledEndDate(scheduledEndTime);
											todo.setJP_ToDo_ScheduledEndTime(scheduledEndTime);
											if(!todo.save())
											{
												//TODO エラー処理
											}
										}

										for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
										{
											receiveToDoCalendarEvent.refresh(p_iToDo);
										}
									}
								}

						};
						FDialog.ask(i_PersonalToDoPopupwindowCaller.getWindowNo(), null,"JP_ToDo_Update_CreatedRepeatedly1", Msg.getMsg(ctx, "JP_ToDo_Update_CreatedRepeatedly2"), isRelaredToDoUpdate);
					}

				}

			}

		}//Update Related ToDo

		return true;
	}


	private boolean deleteToDo()
	{
		//Delete Receiver before p_iToDo.delete() method, because p_iToDo become null after p_iToDo.delete() method;
		for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
		{
			receiveToDoCalendarEvent.delete(p_iToDo);
		}

		//Update Related ToDo
		if(p_iToDo instanceof MToDo)//TODO
		{
			MToDo m_ToDo =	(MToDo)p_iToDo;
			ArrayList<MToDo> list = MToDo.getRelatedToDos(ctx, m_ToDo, null, m_ToDo.getJP_ToDo_ScheduledStartTime(), true, null);

			if(list.size() > 0)
			{
				Callback<Boolean> isRelaredToDoUpdate = new Callback<Boolean>()
				{
						@Override
						public void onCallback(Boolean result)
						{
							if(result)
							{
								for(MToDo todo : list)
								{

									for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
									{
										receiveToDoCalendarEvent.delete(todo);
									}

									if(!todo.delete(false))
									{
										//TODO エラー処理
									}
								}
							}
						}
				};
				FDialog.ask(i_PersonalToDoPopupwindowCaller.getWindowNo(), null,"JP_ToDo_Update_CreatedRepeatedly1", Msg.getMsg(ctx, "JP_ToDo_Update_CreatedRepeatedly2"), isRelaredToDoUpdate);//TODO
			}
		}else if(p_iToDo instanceof MToDoTeam) {

			MToDoTeam m_TeamToDo =	(MToDoTeam)p_iToDo;
			ArrayList<MToDoTeam> list = MToDoTeam.getRelatedTeamToDos(ctx, m_TeamToDo, null, m_TeamToDo.getJP_ToDo_ScheduledStartTime(), true, null);

			if(list.size() > 0)
			{
				Callback<Boolean> isRelaredToDoUpdate = new Callback<Boolean>()
				{
						@Override
						public void onCallback(Boolean result)
						{
							if(result)
							{
								for(MToDoTeam todo : list)
								{

									for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
									{
										receiveToDoCalendarEvent.delete(todo);
									}

									if(!todo.delete(false))
									{
										//TODO エラー処理
									}
								}
							}
						}

				};
				FDialog.ask(i_PersonalToDoPopupwindowCaller.getWindowNo(), null,"JP_ToDo_Update_CreatedRepeatedly1", Msg.getMsg(ctx, "JP_ToDo_Update_CreatedRepeatedly2"), isRelaredToDoUpdate);//TODO
			}

		}//Update Related ToDo

		p_iToDo.delete(false);
		p_iToDo = null;
		p_ParentTeamToDo = null;
		list_ToDoes.remove(index);

		if(index >= list_ToDoes.size())
			index--;

		if(index >= 0 && list_ToDoes.size() > 0)
		{
			p_IsDirty = false;
			p_iToDo = list_ToDoes.get(index);
			p_I_ToDo_ID = p_iToDo.get_ID();
			updateControlParameter(p_I_ToDo_ID);
			updateWindowTitle();
			updateEditorValue();
			updateEditorStatus();
			updateNorth();
			updateCenter();

		}else {

			this.detach();
		}

		return true;
	}

	private void createProcessPopupWindow()
	{
		if(popup == null)
		{
			popup = new Popup();
			popup.setWidgetAttribute(AdempiereWebUI.WIDGET_INSTANCE_NAME, "processButtonPopup");

			String whereClause = " AD_Table_ID=? AND AD_Reference_ID = ? AND AD_Process_ID IS NOT NULL";
			String orderClause ="";

			List<MColumn> list = null;
			if(p_IsPersonalToDo)
			{
				list = new Query(ctx, MColumn.Table_Name, whereClause, null)
						.setParameters(MTable.getTable_ID(MToDo.Table_Name), DisplayType.Button)
						.setOrderBy(orderClause)
						.list();
			}else {

				list = new Query(ctx, MColumn.Table_Name, whereClause, null)
						.setParameters(MTable.getTable_ID(MToDoTeam.Table_Name), DisplayType.Button)
						.setOrderBy(orderClause)
						.list();
			}

			Grid grid = GridFactory.newGridLayout();
			ZKUpdateUtil.setVflex(grid, "min");
			ZKUpdateUtil.setHflex(grid, "min");
			popup.appendChild(grid);
			Rows rows = grid.newRows();
			Row row = null;

			Button btn = null;
			for(MColumn column : list)
			{
				MProcess process = MProcess.get(ctx, column.getAD_Process_ID());

				row = rows.newRow();

				btn = new Button();
				btn.setImage(ThemeManager.getThemeResource("images/Process16.png"));
				btn.setClass("btn-small");
				btn.setStyle("text-align: left");
				btn.setName(BUTTON_KICK_PROCESS);
				btn.setLabel(process.get_Translation("Name"));
				btn.setAttribute("AD_Process_ID", column.getAD_Process_ID());
				btn.addEventListener(Events.ON_CLICK, this);

				ZKUpdateUtil.setHflex(btn, "true");
				row.appendCellChild(btn);

			}

			if(row == null)
			{
				return ;
			}

		}

		popup.setPage(processBtn.getPage());
		popup.open(processBtn, "after_start");

		return ;
	}


	@Override
	public void valueChange(ValueChangeEvent evt)
	{
		String name = evt.getPropertyName();
		Object value = evt.getNewValue();

		if(MToDo.COLUMNNAME_JP_ToDo_Type.equals(name))
		{
			if(value != null)
				p_JP_ToDo_Type = (String)value;

			updateCenter();

		}else if(MToDo.COLUMNNAME_AD_User_ID.equals(name)) {

			String validationCode = null;
			if(evt.getNewValue()==null)
			{
				validationCode = "JP_ToDo_Category.AD_User_ID IS NULL";
			}else {
				validationCode = "JP_ToDo_Category.AD_User_ID IS NULL OR JP_ToDo_Category.AD_User_ID=" + (Integer)evt.getNewValue();
			}

			MLookup JP_ToDo_Category_ID = MLookupFactory.get(Env.getCtx(), 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Category_ID),  DisplayType.Search);
			JP_ToDo_Category_ID.getLookupInfo().ValidationCode = validationCode;
			WSearchEditor editor_JP_ToDo_Category_ID = new WSearchEditor(JP_ToDo_Category_ID, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Category_ID), null, true, p_IsNewRecord? false : true, true);
			map_Editor.put(MToDo.COLUMNNAME_JP_ToDo_Category_ID,editor_JP_ToDo_Category_ID);
			updateCenter();

		}else if(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate.equals(name) || MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime.equals(name)) {

			if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type))
			{
				WDateEditor scheduledStartDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
				Timestamp ts_ScheduledStartDate =(Timestamp)scheduledStartDate.getValue();
				if(ts_ScheduledStartDate == null)
				{
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
					throw new WrongValueException(scheduledStartDate.getComponent(), msg);
				}

				WTimeEditor scheduledStartTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
				Timestamp ts_ScheduledStartTime =(Timestamp)scheduledStartTime.getValue();
				if(ts_ScheduledStartTime == null)
				{
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
					throw new WrongValueException(scheduledStartDate.getComponent(), msg);
				}
				ts_ScheduledStartTime = Timestamp.valueOf(LocalDateTime.of(ts_ScheduledStartDate.toLocalDateTime().toLocalDate(), ts_ScheduledStartTime.toLocalDateTime().toLocalTime()));
				scheduledStartDate.setValue(ts_ScheduledStartTime);
				scheduledStartTime.setValue(ts_ScheduledStartTime);

				WDateEditor scheduledEndDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
				Timestamp ts_ScheduledEndDate =(Timestamp)scheduledEndDate.getValue();
				if(ts_ScheduledEndDate != null)
				{
					if(ts_ScheduledStartTime.compareTo(ts_ScheduledEndDate) >= 0 )
					{
						WTimeEditor scheduledEndTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
						scheduledEndDate.setValue(ts_ScheduledStartTime);
						scheduledEndTime.setValue(ts_ScheduledStartTime);
					}
				}

			}

		}else if(MToDo.COLUMNNAME_IsStartDateAllDayJP.equals(name)) {

			updateCenter();

		}else if(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate.equals(name) || MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime.equals(name)) {//TODO

			if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type) || MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type))
			{
				WDateEditor scheduledEndDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
				Timestamp ts_ScheduledEndDate =(Timestamp)scheduledEndDate.getValue();
				if(ts_ScheduledEndDate == null)
				{
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
					throw new WrongValueException(scheduledEndDate.getComponent(), msg);
				}

				WTimeEditor scheduledEndTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledEndTime);
				Timestamp ts_ScheduledEndTime =(Timestamp)scheduledEndTime.getValue();
				if(ts_ScheduledEndTime == null)
				{
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_JP_ToDo_ScheduledEndDate);
					throw new WrongValueException(scheduledEndDate.getComponent(), msg);
				}
				ts_ScheduledEndTime = Timestamp.valueOf(LocalDateTime.of(ts_ScheduledEndDate.toLocalDateTime().toLocalDate(), ts_ScheduledEndTime.toLocalDateTime().toLocalTime()));
				scheduledEndDate.setValue(ts_ScheduledEndTime);
				scheduledEndTime.setValue(ts_ScheduledEndTime);

				if(MToDo.JP_TODO_TYPE_Task.equals(p_JP_ToDo_Type))
				{
					map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate).setValue(ts_ScheduledEndTime);
					map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime).setValue(ts_ScheduledEndTime);

				}else if(MToDo.JP_TODO_TYPE_Schedule.equals(p_JP_ToDo_Type)) {

					WDateEditor scheduledStartDate = (WDateEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartDate);
					Timestamp ts_ScheduledStartDate =(Timestamp)scheduledStartDate.getValue();
					if(ts_ScheduledStartDate != null)
					{
						if(ts_ScheduledStartDate.compareTo(ts_ScheduledEndTime) >= 0 )
						{
							WTimeEditor scheduledStartTime = (WTimeEditor)map_Editor.get(MToDo.COLUMNNAME_JP_ToDo_ScheduledStartTime);
							scheduledStartDate.setValue(ts_ScheduledEndTime);
							scheduledStartTime.setValue(ts_ScheduledEndTime);
						}

					}

				}
			}

		}else if(MToDo.COLUMNNAME_IsEndDateAllDayJP.equals(name)) {

			updateCenter();
		}

		p_IsDirty = true;
		updateNorth();
	}

	@Override
	public void onClose()
	{
		if(p_IsDirty)
		{

			FDialog.ask(0, null, "SaveChanges?", new Callback<Boolean>() {//Do you want to save changes?

				@Override
				public void onCallback(Boolean result)
				{
					if (result)
					{
						if(!saveToDo())
							return ;

					}else{
						;
					}

					if(isKickedProcess)
					{
						for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
						{
							receiveToDoCalendarEvent.refresh(p_iToDo);
						}
					}

					detach();
		        }

			});//FDialog.

		}else {

			if(isKickedProcess)
			{
				for(I_ToDoCalendarEventReceiver receiveToDoCalendarEvent : list_ToDoCalendarEventReceiver)
				{
					receiveToDoCalendarEvent.refresh(p_iToDo);
				}
			}

			detach();

		}


	}

}
