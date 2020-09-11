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

package jpiere.plugin.groupware.form;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.editor.WYesNoEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRefList;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Vlayout;

import jpiere.plugin.groupware.model.I_ToDo;
import jpiere.plugin.groupware.model.MGroupwareUser;
import jpiere.plugin.groupware.model.MTeam;
import jpiere.plugin.groupware.model.MTeamMember;
import jpiere.plugin.groupware.model.MToDo;
import jpiere.plugin.groupware.model.MToDoCategory;
import jpiere.plugin.groupware.model.MToDoTeam;
import jpiere.plugin.groupware.util.GroupwareToDoUtil;
import jpiere.plugin.groupware.window.I_ToDoCalendarEventReceiver;
import jpiere.plugin.groupware.window.I_ToDoPopupwindowCaller;
import jpiere.plugin.groupware.window.ToDoPopupWindow;;

/**
 *
 * JPIERE-0477: ToDo Daily List
 *
 * h.hagiwara
 *
 */
public class ToDoDailyList implements I_ToDoPopupwindowCaller, I_ToDoCalendarEventReceiver, IFormController, EventListener<Event>, ValueChangeListener {

	//private static CLogger log = CLogger.getCLogger(ToDoCalendar.class);

	private CustomForm form;

	private Properties ctx = Env.getCtx();

	@Override
	public ADForm getForm()
	{
		return form;
	}

	/** ToDo Controler **/

	//HashMap<AD_User_ID, HashMap<JP_ToDo_ID, CalendarEvent>>
	private HashMap<Integer,HashMap<Integer,ToDoCalendarEvent>> map_AcquiredCalendarEvent_User = new HashMap<Integer,HashMap<Integer,ToDoCalendarEvent>>();
	private HashMap<Integer,HashMap<Integer,ToDoCalendarEvent>> map_AcquiredCalendarEvent_Team = new HashMap<Integer,HashMap<Integer,ToDoCalendarEvent>>();


	/** Parameters **/
	private int p_login_User_ID = 0;
	private int p_AD_User_ID = 0;
	private int p_SelectedTab_AD_User_ID = 0;
	private int p_OldSelectedTab_AD_User_ID = 0;

	private int p_JP_Team_ID = 0;
	private MTeam m_Team = null;

	private int p_JP_ToDo_Category_ID = 0;
	private String p_JP_ToDo_Status = null ;
	private boolean p_IsDisplaySchedule = true;
	private boolean p_IsDisplayTask = false;

	private String p_JP_FirstDayOfWeek = MGroupwareUser.JP_FIRSTDAYOFWEEK_Sunday;
	private String p_JP_ToDo_Main_Calendar = MGroupwareUser.JP_TODO_MAIN_CALENDAR_IncludeTeamMemberSToDo;
	private String p_JP_ToDo_Calendar = MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo;

	private String p_CalendarMold = null;

	private MGroupwareUser m_GroupwareUser = null;


	/** Noth Components **/
	private WSearchEditor editor_AD_User_ID;
	private WSearchEditor editor_JP_ToDo_Category_ID;
	private WSearchEditor editor_JP_Team_ID ;
	private WYesNoEditor  editor_IsDisplaySchedule ;
	private WYesNoEditor  editor_IsDisplayTask ;
	private WTableDirEditor editor_JP_ToDo_Calendar ;

	private MLookup lookup_JP_ToDo_Category_ID;
	private MLookup lookup_JP_ToDo_Calendar;

	private Button button_Customize;


	/** Center **/
	private Center mainBorderLayout_Center;

	private Tabbox tabbox;
	private Tab tab_p_AD_User_ID;
	private Tabpanel tabpanel_p_AD_User_ID;


	/** West Components **/

	private Tabbox westTabbox;

	ToDoGadget personalToDoGadget_Schedule = null;
	ToDoGadget personalToDoGadget_Task = null;
	ToDoGadget personalToDoGadget_Memo = null;

	ToDoGadget teamToDoGadget_Schedule = null;
	ToDoGadget teamToDoGadget_Task = null;
	ToDoGadget teamToDoGadget_Memo = null;


	//Popup
	private CalendarEventPopup popup_CalendarEvent = new CalendarEventPopup();
	private Popup popup_Customize = null;

	private WTableDirEditor editor_JP_FirstDayOfWeek ;
	private WNumberEditor   editor_JP_ToDo_Calendar_BeginTime ;
	private WNumberEditor   editor_JP_ToDo_Calendar_EndTime ;
	private WYesNoEditor    editor_IsDisplaySchedule_For_Custom ;
	private WYesNoEditor    editor_IsDisplayTask_For_Custom ;

	private WTableDirEditor editor_JP_ToDo_Main_Calendar ;
	private WTableDirEditor editor_JP_ToDo_Calendar_For_Custom ;

	private Button button_Customize_Save;


	/** Label **/
	private Label label_AD_User_ID ;
	private Label label_JP_ToDo_Category_ID ;
	private Label label_JP_Team_ID ;
	private Label label_DisplayPeriod;

	private Label label_JP_FirstDayOfWeek;
	private Label label_JP_ToDo_Calendar_BeginTime;
	private Label label_JP_ToDo_Calendar_EndTime;
	private Label label_JP_ToDo_Main_Calendar;
	private Label label_JP_ToDo_Calendar;
	private Label label_JP_ToDo_Calendar_For_Custom;


	/** Interface **/
	private List<I_ToDo> list_ToDoes = null;





	//Statics
	public final static String BUTTON_PREVIOUS = "PREVIOUS";
	public final static String BUTTON_NEXT = "NEXT";
	public final static String BUTTON_NEW = "NEW";
	public final static String BUTTON_TODAY = "TODAY";

	public final static String BUTTON_REFRESH = "REFRESH";
	public final static String BUTTON_CUSTOMIZE = "CUSTOMIZE";
	public final static String BUTTON_CUSTOMIZE_SAVE = "CUSTOMIZE_SAVE";

	public final static String JP_TODO_CALENDAR_MAX_MEMBER ="JP_TODO_CALENDAR_MAX_MEMBER";
	public final static String CSS_DEFAULT_TAB_STYLE ="border-top: 4px solid #ACD5EE;";


	/**
	 * Constructor
	 */
    public ToDoDailyList()
    {
		p_AD_User_ID = Env.getAD_User_ID(ctx);
		p_login_User_ID = p_AD_User_ID;
		p_SelectedTab_AD_User_ID = p_AD_User_ID;
		p_OldSelectedTab_AD_User_ID = p_AD_User_ID;

		initZk();

		updateDateLabel();

    }



    /**
     * Create Initial Main Calendar
     *
     * Create initial main calendar. Get default Value from Groupware user if any.
     *
     * @return Calendars
     */
    private Calendars createInitialMainCalendar()
    {
    	Calendars calendars = new Calendars();

		calendars.invalidate();

		calendars.addEventListener(CalendarsEvent.ON_EVENT_CREATE, this);
		calendars.addEventListener(CalendarsEvent.ON_EVENT_EDIT, this);
		calendars.addEventListener(CalendarsEvent.ON_EVENT_UPDATE,this);
		calendars.addEventListener("onMouseOver", this);
		calendars.addEventListener(CalendarsEvent.ON_DAY_CLICK,this);
		//calendars.addEventListener(CalendarsEvent.ON_WEEK_CLICK, this);
		calendars.addEventListener(CalendarsEvent.ON_EVENT_TOOLTIP, this);


		if(m_GroupwareUser == null)
		{
			m_GroupwareUser = MGroupwareUser.get(ctx, p_login_User_ID);

			if(m_GroupwareUser != null)
			{
				p_IsDisplaySchedule = m_GroupwareUser.isDisplayScheduleJP();
				p_IsDisplayTask = m_GroupwareUser.isDisplayTaskJP();
				calendars.setBeginTime(m_GroupwareUser.getJP_ToDo_Calendar_BeginTime());
				calendars.setEndTime(m_GroupwareUser.getJP_ToDo_Calendar_EndTime());

				String fdow = m_GroupwareUser.getJP_FirstDayOfWeek();
				if(!Util.isEmpty(fdow))
				{
					int AD_Column_ID = MColumn.getColumn_ID(MGroupwareUser.Table_Name, MGroupwareUser.COLUMNNAME_JP_FirstDayOfWeek);
					int AD_Reference_Value_ID = MColumn.get(ctx, AD_Column_ID).getAD_Reference_Value_ID();
					MRefList refList =MRefList.get(ctx, AD_Reference_Value_ID, fdow,null);
					p_JP_FirstDayOfWeek = refList.getValue();
					calendars.setFirstDayOfWeek(refList.getName());
				}

				p_JP_ToDo_Main_Calendar = m_GroupwareUser.getJP_ToDo_Main_Calendar();
				p_JP_ToDo_Calendar = m_GroupwareUser.getJP_ToDo_Calendar();

			}
		}

		//set Calendar Mold
		if(m_GroupwareUser != null && !Util.isEmpty(m_GroupwareUser.getJP_DefaultCalendarView()))
		{
			String calendarView = m_GroupwareUser.getJP_DefaultCalendarView();
			if(MGroupwareUser.JP_DEFAULTCALENDARVIEW_Month.equals(calendarView))
			{
				p_CalendarMold = GroupwareToDoUtil.CALENDAR_MONTH_VIEW;
				calendars.setMold("month");
				calendars.setDays(0);

			}else if(MGroupwareUser.JP_DEFAULTCALENDARVIEW_Week.equals(calendarView)){

				p_CalendarMold = GroupwareToDoUtil.CALENDAR_SEVENDAYS_VIEW;
				calendars.setMold("default");
				calendars.setDays(7);

			}else if(MGroupwareUser.JP_DEFAULTCALENDARVIEW_FiveDays.equals(calendarView)){

				p_CalendarMold = GroupwareToDoUtil.CALENDAR_FIVEDAYS_VIEW;
				calendars.setMold("default");
				calendars.setDays(5);
			}else if(MGroupwareUser.JP_DEFAULTCALENDARVIEW_Day.equals(calendarView)){

				p_CalendarMold = GroupwareToDoUtil.CALENDAR_ONEDAY_VIEW;
				calendars.setMold("default");
				calendars.setDays(1);
			}

		}else {

			p_CalendarMold  = GroupwareToDoUtil.CALENDAR_SEVENDAYS_VIEW;
			calendars.setMold("default");
			calendars.setDays(7);
		}

		return calendars;
    }



    /**
     * Initialization ZK
     */
    private void initZk()
    {
    	form = new CustomForm();
    	Borderlayout mainBorderLayout = new Borderlayout();
    	form.appendChild(mainBorderLayout);

		ZKUpdateUtil.setWidth(mainBorderLayout, "99%");
		ZKUpdateUtil.setHeight(mainBorderLayout, "100%");


		//***************** NORTH **************************//

		North mainBorderLayout_North = new North();
		mainBorderLayout_North.setSplittable(false);
		mainBorderLayout_North.setCollapsible(false);
		mainBorderLayout_North.setOpen(true);
		mainBorderLayout.appendChild(mainBorderLayout_North);
		mainBorderLayout_North.appendChild(createNorthContents());


		//***************** CENTER **************************//

		mainBorderLayout_Center = new Center();
		mainBorderLayout.appendChild(mainBorderLayout_Center);
		mainBorderLayout_Center.appendChild(createCenterContents());


    }



    /**
     * Create Noth contents of Borderlayout.
     *
     * @return Div
     */
    private Div createNorthContents()
    {
    	Div outerDiv = new Div();
		ZKUpdateUtil.setVflex(outerDiv, "max");
		ZKUpdateUtil.setHflex(outerDiv, "max");
    	outerDiv.setStyle("padding:4px 2px 4px 2px; margin-bottom:4px; border: solid 2px #dddddd;");
    	Vlayout vlayout = new Vlayout();
    	outerDiv.appendChild(vlayout);


		Grid grid = GridFactory.newGridLayout();
		ZKUpdateUtil.setVflex(grid, "max");
		ZKUpdateUtil.setHflex(grid, "min");
		vlayout.appendChild(grid);

		Rows rows = grid.newRows();
		Row row = rows.newRow();

		row.appendChild(GroupwareToDoUtil.getDividingLine());

		//User Search
		MLookup lookupUser = MLookupFactory.get(ctx, 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_AD_User_ID),  DisplayType.Search);
		editor_AD_User_ID = new WSearchEditor(MToDo.COLUMNNAME_AD_User_ID, true, false, true, lookupUser);
		editor_AD_User_ID.setValue(p_AD_User_ID);
		editor_AD_User_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_AD_User_ID.getComponent(), "true");

		label_AD_User_ID = new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_AD_User_ID));
		label_AD_User_ID.setId(MToDo.COLUMNNAME_AD_User_ID);
		label_AD_User_ID.addEventListener(Events.ON_CLICK, this);

		row.appendChild(GroupwareToDoUtil.createLabelDiv(editor_AD_User_ID, label_AD_User_ID, true));
		row.appendChild(editor_AD_User_ID.getComponent());
		editor_AD_User_ID.showMenu();


		row.appendChild(GroupwareToDoUtil.createSpaceDiv());


		//ToDo Category Search
		lookup_JP_ToDo_Category_ID = MLookupFactory.get(ctx, 0,  0, MColumn.getColumn_ID(MToDo.Table_Name, MToDo.COLUMNNAME_JP_ToDo_Category_ID),  DisplayType.Search);
		String validationCode = null;
		if(p_AD_User_ID == 0)
		{
			validationCode = "JP_ToDo_Category.AD_User_ID IS NULL";
		}else {
			validationCode = "JP_ToDo_Category.AD_User_ID IS NULL OR JP_ToDo_Category.AD_User_ID=" + p_AD_User_ID;
		}

		lookup_JP_ToDo_Category_ID.getLookupInfo().ValidationCode = validationCode;
		editor_JP_ToDo_Category_ID = new WSearchEditor(MToDo.COLUMNNAME_JP_ToDo_Category_ID, false, false, true, lookup_JP_ToDo_Category_ID);
		editor_JP_ToDo_Category_ID.setValue(null);
		editor_JP_ToDo_Category_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_ToDo_Category_ID.getComponent(), "true");


		label_JP_ToDo_Category_ID = new Label(Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Category_ID));
		label_JP_ToDo_Category_ID.addEventListener(Events.ON_CLICK, this);

		row.appendChild(GroupwareToDoUtil.createLabelDiv(editor_JP_ToDo_Category_ID, label_JP_ToDo_Category_ID, true));
		row.appendChild(editor_JP_ToDo_Category_ID.getComponent());
		editor_JP_ToDo_Category_ID.showMenu();

		row.appendChild(GroupwareToDoUtil.createSpaceDiv());

		//ToDo Status List
		MLookup lookup_JP_ToDo_Status = MLookupFactory.get(ctx, 0,  0, MColumn.getColumn_ID(MGroupwareUser.Table_Name, MGroupwareUser.COLUMNNAME_JP_ToDo_Status),  DisplayType.List);
		WTableDirEditor editor_JP_ToDo_Status = new WTableDirEditor(MToDo.COLUMNNAME_JP_ToDo_Status, false, false, true, lookup_JP_ToDo_Status);
		editor_JP_ToDo_Status.addValueChangeListener(this);
		//ZKUpdateUtil.setHflex(editor_JP_ToDo_Status.getComponent(), "true");

		row.appendChild(GroupwareToDoUtil.createLabelDiv(editor_JP_ToDo_Status, Msg.getElement(ctx, MToDo.COLUMNNAME_JP_ToDo_Status), true));
		row.appendChild(editor_JP_ToDo_Status.getComponent());


		row.appendChild(GroupwareToDoUtil.createSpaceDiv());


		//Team Searh
		MLookup lookupTeam = MLookupFactory.get(ctx, 0,  0, MColumn.getColumn_ID(MToDoTeam.Table_Name, MTeam.COLUMNNAME_JP_Team_ID),  DisplayType.Search);
		editor_JP_Team_ID = new WSearchEditor( MTeam.COLUMNNAME_JP_Team_ID, false, false, true, lookupTeam);
		editor_JP_Team_ID.setValue(p_JP_Team_ID);
		editor_JP_Team_ID.addValueChangeListener(this);
		ZKUpdateUtil.setHflex(editor_JP_Team_ID.getComponent(), "true");

		label_JP_Team_ID = new Label(Msg.getElement(ctx, MTeam.COLUMNNAME_JP_Team_ID));
		label_JP_Team_ID.addEventListener(Events.ON_CLICK, this);

		row.appendChild(GroupwareToDoUtil.createLabelDiv(editor_JP_Team_ID, label_JP_Team_ID, true));
		row.appendChild(editor_JP_Team_ID.getComponent());
		editor_JP_Team_ID.showMenu();


		row.appendChild(GroupwareToDoUtil.getDividingLine());
		row.appendChild(GroupwareToDoUtil.createSpaceDiv());


		editor_IsDisplaySchedule = new WYesNoEditor(MGroupwareUser.COLUMNNAME_IsDisplayScheduleJP, Msg.getElement(ctx,MGroupwareUser.COLUMNNAME_IsDisplayScheduleJP), null, true, false, true);
		editor_IsDisplaySchedule.setValue(p_IsDisplaySchedule);
		editor_IsDisplaySchedule.addValueChangeListener(this);
		row.appendChild(GroupwareToDoUtil.createEditorDiv(editor_IsDisplaySchedule, true));


		editor_IsDisplayTask = new WYesNoEditor(MGroupwareUser.COLUMNNAME_IsDisplayTaskJP, Msg.getElement(ctx,MGroupwareUser.COLUMNNAME_IsDisplayTaskJP), null, true, false, true);
		editor_IsDisplayTask.setValue(p_IsDisplayTask);
		editor_IsDisplayTask.addValueChangeListener(this);
		row.appendChild(GroupwareToDoUtil.createEditorDiv(editor_IsDisplayTask, true));


		/******************** 2nd floor *********************************/

		grid = GridFactory.newGridLayout();
		ZKUpdateUtil.setVflex(grid, "max");
		ZKUpdateUtil.setHflex(grid, "min");
		vlayout.appendChild(grid);

		rows = grid.newRows();
		row = rows.newRow();

		row.appendChild(GroupwareToDoUtil.getDividingLine());

		//JP_ToDo_Calendar
		lookup_JP_ToDo_Calendar = MLookupFactory.get(ctx, 0,  0, MColumn.getColumn_ID(MGroupwareUser.Table_Name, MGroupwareUser.COLUMNNAME_JP_ToDo_Calendar),  DisplayType.List);
		editor_JP_ToDo_Calendar= new WTableDirEditor(MGroupwareUser.COLUMNNAME_JP_ToDo_Calendar, true, false, true, lookup_JP_ToDo_Calendar);
		editor_JP_ToDo_Calendar.setValue(p_JP_ToDo_Calendar);
		editor_JP_ToDo_Calendar.addValueChangeListener(this);
		label_JP_ToDo_Calendar = new Label(Msg.getElement(ctx, MGroupwareUser.COLUMNNAME_JP_ToDo_Calendar));
		row.appendChild(GroupwareToDoUtil.createLabelDiv(editor_JP_ToDo_Calendar, label_JP_ToDo_Calendar, true));
		row.appendChild(editor_JP_ToDo_Calendar.getComponent());

		row.appendChild(GroupwareToDoUtil.getDividingLine());

		//Create New ToDo Button
		Button createNewToDo = new Button();
		createNewToDo.setImage(ThemeManager.getThemeResource("images/New16.png"));
		createNewToDo.setName(BUTTON_NEW);
		createNewToDo.addEventListener(Events.ON_CLICK, this);
		createNewToDo.setId(String.valueOf(0));
		createNewToDo.setLabel(Msg.getMsg(ctx, "NewRecord"));
		ZKUpdateUtil.setHflex(createNewToDo, "true");
		row.appendCellChild(createNewToDo,2);
		row.appendChild(GroupwareToDoUtil.createSpaceDiv());

		//Refresh Button
		Button refresh = new Button();
		refresh.setImage(ThemeManager.getThemeResource("images/Refresh16.png"));
		refresh.setName(BUTTON_REFRESH);
		refresh.addEventListener(Events.ON_CLICK, this);
		refresh.setLabel(Msg.getMsg(ctx, "Refresh"));
		ZKUpdateUtil.setHflex(refresh, "true");
		row.appendCellChild(refresh, 2);


		row.appendChild(GroupwareToDoUtil.getDividingLine());


		label_DisplayPeriod = new Label();
		updateDateLabel();

		row.appendChild(GroupwareToDoUtil.createLabelDiv(null,  Msg.getMsg(ctx, "JP_DisplayPeriod") + " : ", true));
		row.appendChild(GroupwareToDoUtil.createLabelDiv(null, label_DisplayPeriod, true));

		row.appendChild(GroupwareToDoUtil.createSpaceDiv());
		row.appendChild(GroupwareToDoUtil.getDividingLine());
		row.appendChild(GroupwareToDoUtil.createSpaceDiv());

    	return outerDiv;

    }





    /**
     * Create Center contents of Borderlayout.
     *
     * @return Div
     */
    private Div createCenterContents()
    {
       	Div div = new Div();
    		Vlayout vlayout = new Vlayout();
    		vlayout.setDroppable("false");
    		div.appendChild(vlayout);
    		//div.setStyle("overflow-y:scroll;");

    		ZKUpdateUtil.setWidth(div, "100%");
    		ZKUpdateUtil.setHeight(div, "100%");
    		ZKUpdateUtil.setHflex(div, "1");
    		ZKUpdateUtil.setVflex(div, "1");

    		ZKUpdateUtil.setWidth(vlayout, "100%");
    		ZKUpdateUtil.setHeight(vlayout, "100%");
    		ZKUpdateUtil.setHflex(vlayout, "1");
    		ZKUpdateUtil.setVflex(vlayout, "1");
    		vlayout.setStyle("overflow-y:scroll;");


    	for(int i =0; i <10; i++)
    	{
    		//Unfinished Tasks
    		Groupbox groupBox2 = new Groupbox();
    		groupBox2.setOpen(true);
    		groupBox2.setDraggable("false");
    		groupBox2.setMold("3d");
    		groupBox2.setWidgetListener("onOpen", "this.caption.setIconSclass('z-icon-caret-' + (event.open ? 'down' : 'right'));");

    		vlayout.appendChild(groupBox2);


    		Caption caption2 = new Caption("ユーザーXXXX");
    		caption2.setIconSclass("z-icon-caret-down");
    		groupBox2.appendChild(caption2);

    		//大枠
    		Hlayout hlayout = new Hlayout();
    		hlayout.setDroppable("false");
    		groupBox2.appendChild(hlayout);
    		hlayout.setStyle("padding:4px 2px 4px 2px; margin-bottom:4px; border: solid 2px #dddddd;");


    		//*********************1

    		Vlayout day1 = new Vlayout();
    		hlayout.appendChild(day1);
    		ZKUpdateUtil.setHflex(day1, "1");
    		day1.setStyle("padding:2px 2px 2px 2px; margin-bottom:4px; border: solid 2px #dddddd;");

    		Div day1_header = new Div();
    		Label label = new Label("2020/09/14");
    		label.setStyle("text-align: center; color:#ffffff ");
    		day1_header.appendChild(label);
    		day1_header.setStyle("padding:4px 2px 4px 4px; background-color:#003894;");
    		day1.appendChild(day1_header);

    		Div day1_Content = new Div();
    		personalToDoGadget_Task = new ToDoGadget(MToDo.JP_TODO_TYPE_Memo, MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo);
    		personalToDoGadget_Task.setToDoPopupwindowCaller(this);
    		personalToDoGadget_Task.addToDoCalenderEventReceiver(this);
    		day1_Content.appendChild(personalToDoGadget_Task);
    		day1.appendChild(day1_Content);


    		//*********************2
    		Vlayout day2 = new Vlayout();
    		hlayout.appendChild(day2);
    		ZKUpdateUtil.setHflex(day2, "1");
    		day2.setStyle("padding:2px 2px 2px 2px; margin-bottom:4px; border: solid 2px #dddddd;");

    		Div day2_header = new Div();
    		Label label2 = new Label("2020/09/15");
    		label2.setStyle("text-align: center; color:#ffffff ");
    		day2_header.appendChild(label2);
    		day2_header.setStyle("padding:4px 2px 4px 4px; background-color:#003894;");
    		day2.appendChild(day2_header);

    		Div day2_Content = new Div();
    		personalToDoGadget_Memo= new ToDoGadget(MToDo.JP_TODO_TYPE_Memo, MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo);
    		personalToDoGadget_Memo.setToDoPopupwindowCaller(this);
    		personalToDoGadget_Memo.addToDoCalenderEventReceiver(this);
    		day2_Content.appendChild(personalToDoGadget_Memo);
    		day2.appendChild(day2_Content);


    		//*********************3
    		Vlayout day3 = new Vlayout();
    		hlayout.appendChild(day3);
    		ZKUpdateUtil.setHflex(day3, "1");
    		day3.setStyle("padding:2px 2px 2px 2px; margin-bottom:4px; border: solid 2px #dddddd;");


    		Div day3_header = new Div();
    		Label label3 = new Label("2020/09/16");
    		label3.setStyle("text-align: center; color:#ffffff ");
    		day3_header.appendChild(label3);
    		day3_header.setStyle("padding:4px 2px 4px 4px; background-color:#003894;");
    		day3.appendChild(day3_header);


    		Div day3_Content = new Div();
    		ToDoGadget day3_todo= new ToDoGadget(MToDo.JP_TODO_TYPE_Memo, MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo);
    		day3_todo.setToDoPopupwindowCaller(this);
    		day3_todo.addToDoCalenderEventReceiver(this);
    		day3_Content.appendChild(day3_todo);
    		day3.appendChild(day3_Content);


    		//*********************4
    		Vlayout day4 = new Vlayout();
    		hlayout.appendChild(day4);
    		ZKUpdateUtil.setHflex(day4, "1");
    		day4.setStyle("padding:2px 2px 2px 2px; margin-bottom:4px; border: solid 2px #dddddd;");


    		Div day4_header = new Div();
    		Label label4 = new Label("2020/09/17");
    		label4.setStyle("text-align: center; color:#ffffff ");
    		day4_header.appendChild(label4);
    		day4_header.setStyle("padding:4px 2px 4px 4px; background-color:#003894;");
    		day4.appendChild(day4_header);


    		Div day4_Content = new Div();
    		ToDoGadget day4_todo= new ToDoGadget(MToDo.JP_TODO_TYPE_Memo, MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo);
    		day4_todo.setToDoPopupwindowCaller(this);
    		day4_todo.addToDoCalenderEventReceiver(this);
    		day4_Content.appendChild(day4_todo);
    		day4.appendChild(day4_Content);


    		//*********************5
    		Vlayout day5 = new Vlayout();
    		hlayout.appendChild(day5);
    		ZKUpdateUtil.setHflex(day5, "1");
    		day5.setStyle("padding:2px 2px 2px 2px; margin-bottom:4px; border: solid 2px #dddddd;");


    		Div day5_header = new Div();
    		Label label5 = new Label("2020/09/18");
    		label5.setStyle("text-align: center; color:#ffffff ");
    		day5_header.appendChild(label5);
    		day5_header.setStyle("padding:4px 2px 4px 4px; background-color:#003894;");
    		day5.appendChild(day5_header);


    		Div day5_Content = new Div();
    		ToDoGadget day5_todo= new ToDoGadget(MToDo.JP_TODO_TYPE_Memo, MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo);
    		day5_todo.setToDoPopupwindowCaller(this);
    		day5_todo.addToDoCalenderEventReceiver(this);
    		day5_Content.appendChild(day5_todo);
    		day5.appendChild(day5_Content);


    	}
        	return div;

    }




    /**
     * Value Chenge Events
     */
	@Override
	public void valueChange(ValueChangeEvent evt)
	{
		String name = evt.getPropertyName();
		Object value = evt.getNewValue();

		if(MToDo.COLUMNNAME_AD_User_ID.equals(name))
		{

			if(value == null)
			{
				p_AD_User_ID = 0;
				editor_AD_User_ID.setValue(null);

			}else {
				p_AD_User_ID = Integer.parseInt(value.toString());
			}


			String validationCode = null;
			if(evt.getNewValue()==null)
			{
				validationCode = "JP_ToDo_Category.AD_User_ID IS NULL";
			}else {
				validationCode = "JP_ToDo_Category.AD_User_ID IS NULL OR JP_ToDo_Category.AD_User_ID=" + (Integer)evt.getNewValue();
			}

			lookup_JP_ToDo_Category_ID.getLookupInfo().ValidationCode = validationCode;
			editor_JP_ToDo_Category_ID.setValue(null);

			int old_JP_ToDo_Category_ID = p_JP_ToDo_Category_ID;
			p_JP_ToDo_Category_ID = 0;

			if(p_AD_User_ID == 0)
			{
				Object obj = evt.getSource();
				if(obj instanceof WEditor)
				{
					WEditor editor = (WEditor)obj;
					String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MToDo.COLUMNNAME_AD_User_ID);
					throw new WrongValueException(editor.getComponent(), msg);
				}

			}else if(old_JP_ToDo_Category_ID > 0) {

				//The ToDo category have been cleared as the user has changed.
				throw new WrongValueException(editor_JP_ToDo_Category_ID.getComponent(), Msg.getMsg(ctx, "JP_ToDo_Category_Cleared"));

			}

			tabbox = null;
		  	mainBorderLayout_Center.getFirstChild().detach();
			mainBorderLayout_Center.appendChild(createCenterContents());


		}else if(MToDo.COLUMNNAME_JP_ToDo_Category_ID.equals(name)){

			if(value == null)
			{
				p_JP_ToDo_Category_ID = 0;
			}else {
				p_JP_ToDo_Category_ID = Integer.parseInt(value.toString());
			}


		}else if(MTeam.COLUMNNAME_JP_Team_ID.equals(name)){

			if(value == null)
			{
				p_JP_Team_ID = 0;
				m_Team = null;
				tab_p_AD_User_ID.setLabel(MUser.get(ctx, p_AD_User_ID).getName());

			}else {

				p_JP_Team_ID = Integer.parseInt(value.toString());
				m_Team = new MTeam(ctx, p_JP_Team_ID, null);

				MTeamMember[] member = m_Team.getTeamMember();
				int JP_ToDo_Calendar_Max_Member = MSysConfig.getIntValue(JP_TODO_CALENDAR_MAX_MEMBER, 100, Env.getAD_Client_ID(ctx));

				if(member.length == 0 || (member.length == 1 && member[0].getAD_User_ID() == p_AD_User_ID))
				{
					p_JP_Team_ID = 0;
					m_Team = null;
					tab_p_AD_User_ID.setLabel(MUser.get(ctx, p_AD_User_ID).getName());
					editor_JP_Team_ID.setValue(0);

					//There are no users on the team, or there are no users on the team except the selected user.
					FDialog.error(form.getWindowNo(), "Error", Msg.getMsg(ctx, "JP_Team_No_Users_Except_Selected_User"));

					return ;
				}


				if(member.length > JP_ToDo_Calendar_Max_Member)
				{
					p_JP_Team_ID = 0;
					m_Team = null;
					tab_p_AD_User_ID.setLabel(MUser.get(ctx, p_AD_User_ID).getName());
					editor_JP_Team_ID.setValue(0);

					//The number of users belonging to the selected team has exceeded the maximum number of users that can be displayed on the calendar.
					FDialog.error(form.getWindowNo(), "Error", Msg.getMsg(ctx, "JP_ToDo_Calendar_Max_Member", new Object[] {member.length,JP_ToDo_Calendar_Max_Member}));

					return ;
				}

				tab_p_AD_User_ID.setLabel(MUser.get(ctx, p_AD_User_ID).getName() + " & "  + Msg.getElement(ctx, MTeam.COLUMNNAME_JP_Team_ID));

			}

		  	mainBorderLayout_Center.getFirstChild().detach();
			mainBorderLayout_Center.appendChild(createCenterContents());


		}else if(MGroupwareUser.COLUMNNAME_IsDisplayScheduleJP.equals(name)) {

			p_IsDisplaySchedule = (boolean)value;
			editor_IsDisplaySchedule.setValue(value);
			editor_IsDisplaySchedule_For_Custom.setValue(value);
			if(editor_IsDisplaySchedule_For_Custom.isVisible())
				button_Customize_Save.setDisabled(false);

		}else if(MGroupwareUser.COLUMNNAME_IsDisplayTaskJP.equals(name)) {

			p_IsDisplayTask = (boolean)value;
			editor_IsDisplayTask.setValue(value);
			editor_IsDisplayTask_For_Custom.setValue(value);
			if(editor_IsDisplayTask_For_Custom.isVisible())
				button_Customize_Save.setDisabled(false);



		}else if(MToDo.COLUMNNAME_JP_ToDo_Status.equals(name)){

			if(value == null)
			{
				p_JP_ToDo_Status = null;
			}else {

				if(Util.isEmpty(value.toString()))
				{
					p_JP_ToDo_Status = null;
				}else {
					p_JP_ToDo_Status = value.toString();
				}
			}



		}else if(MGroupwareUser.COLUMNNAME_JP_ToDo_Calendar.equals(name)) {

			if(value == null)
			{
				WTableDirEditor comp = (WTableDirEditor)evt.getSource();
				String msg = Msg.getMsg(Env.getCtx(), "FillMandatory") + Msg.getElement(Env.getCtx(), MGroupwareUser.COLUMNNAME_JP_ToDo_Calendar);//
				throw new WrongValueException(comp.getComponent(), msg);
			}

			p_JP_ToDo_Calendar = value.toString();
			editor_JP_ToDo_Calendar.setValue(value);
			editor_JP_ToDo_Calendar_For_Custom.setValue(value);

			p_SelectedTab_AD_User_ID = p_AD_User_ID;
			p_OldSelectedTab_AD_User_ID = p_AD_User_ID;

			if(p_JP_Team_ID > 0)
			{
			  	mainBorderLayout_Center.getFirstChild().detach();
				mainBorderLayout_Center.appendChild(createCenterContents());
			}
			if(editor_JP_ToDo_Calendar_For_Custom.isVisible())
				button_Customize_Save.setDisabled(false);
		}

	}



	/**
	 * Event Process
	 */
	@Override
	public void onEvent(Event event) throws Exception
	{
		Component comp = event.getTarget();
		String eventName = event.getName();
		if(eventName.equals(Events.ON_CLICK))
		{
			if(comp instanceof Button)
			{
				Button btn = (Button) comp;
				String btnName = btn.getName();
				if(BUTTON_NEW.equals(btnName))
				{
					list_ToDoes = null;
					p_CalendarsEventBeginDate = null;
					p_CalendarsEventEndDate =  null;

					ToDoPopupWindow todoWindow = new ToDoPopupWindow(this, -1);
					todoWindow.addToDoCalenderEventReceiver(this);
					todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Schedule);
					todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Task);
					todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Memo);
					todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Schedule);
					todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Task);
					todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Memo);
					SessionManager.getAppDesktop().showWindow(todoWindow);

				}else if(BUTTON_REFRESH.equals(btnName)){

					p_SelectedTab_AD_User_ID = p_AD_User_ID;
					p_OldSelectedTab_AD_User_ID = p_AD_User_ID;

					if(p_JP_Team_ID > 0)
					{
					  	mainBorderLayout_Center.getFirstChild().detach();
						mainBorderLayout_Center.appendChild(createCenterContents());
					}

					refreshWest(null);

			}

			}else if(comp instanceof Label){

				//Zoom AD_User_ID -> Groupware User window
				if(label_AD_User_ID.equals(comp))
				{
					Object value = editor_AD_User_ID.getValue();
					if(value == null || Util.isEmpty(value.toString()))
					{
						AEnv.zoom(MTable.getTable_ID(MGroupwareUser.Table_Name), 0);
					}else {

						if(m_GroupwareUser == null)
						{
							AEnv.zoom(MTable.getTable_ID(MGroupwareUser.Table_Name), 0);

						}else {

							MGroupwareUser gUser = MGroupwareUser.get(ctx, Integer.valueOf(value.toString()));
							AEnv.zoom(MTable.getTable_ID(MGroupwareUser.Table_Name), gUser.getJP_GroupwareUser_ID());
						}
					}

				}else if(label_JP_Team_ID.equals(comp)) {

					Object value = editor_JP_Team_ID.getValue();
					if(value == null || Util.isEmpty(value.toString()))
					{
						AEnv.zoom(MTable.getTable_ID(MTeam.Table_Name), 0);
					}else {
						AEnv.zoom(MTable.getTable_ID(MTeam.Table_Name), Integer.valueOf(value.toString()));
					}

				}else if(label_JP_ToDo_Category_ID.equals(comp)) {

					Object value = editor_JP_ToDo_Category_ID.getValue();
					if(value == null || Util.isEmpty(value.toString()))
					{
						AEnv.zoom(MTable.getTable_ID(MToDoCategory.Table_Name), 0);
					}else {
						AEnv.zoom(MTable.getTable_ID(MToDoCategory.Table_Name), Integer.valueOf(value.toString()));
					}
				}

			}


		}else if (CalendarsEvent.ON_EVENT_CREATE.equals(eventName)) {

			if (event instanceof CalendarsEvent)
			{
				list_ToDoes = null;

				CalendarsEvent calendarsEvent = (CalendarsEvent) event;
				p_CalendarsEventBeginDate = new Timestamp(calendarsEvent.getBeginDate().getTime());
				p_CalendarsEventEndDate = new Timestamp(calendarsEvent.getEndDate().getTime());

				ToDoPopupWindow todoWindow = new ToDoPopupWindow(this, -1);
				todoWindow.addToDoCalenderEventReceiver(this);
				todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Schedule);
				todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Task);
				todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Memo);
				todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Schedule);
				todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Task);
				todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Memo);

				SessionManager.getAppDesktop().showWindow(todoWindow);
			}

		}else if (CalendarsEvent.ON_EVENT_EDIT.equals(eventName)) {

			if (event instanceof CalendarsEvent)
			{
				CalendarsEvent calendarsEvent = (CalendarsEvent) event;
				CalendarEvent calendarEvent = calendarsEvent.getCalendarEvent();

				if (calendarEvent instanceof ToDoCalendarEvent)
				{
					ToDoCalendarEvent ce = (ToDoCalendarEvent) calendarEvent;

					list_ToDoes = new ArrayList<I_ToDo>();
					list_ToDoes.add(ce.getToDo());

					p_CalendarsEventBeginDate = ce.getToDo().getJP_ToDo_ScheduledStartTime();
					p_CalendarsEventEndDate = ce.getToDo().getJP_ToDo_ScheduledEndTime();

					ToDoPopupWindow todoWindow = new ToDoPopupWindow(this, 0);
					todoWindow.addToDoCalenderEventReceiver(this);
					todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Schedule);
					todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Task);
					todoWindow.addToDoCalenderEventReceiver(personalToDoGadget_Memo);
					todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Schedule);
					todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Task);
					todoWindow.addToDoCalenderEventReceiver(teamToDoGadget_Memo);

					SessionManager.getAppDesktop().showWindow(todoWindow);

				}
			}

		//}else if ("onMouseOver".equals(eventName)){

			;//Not Use

		}else if (CalendarsEvent.ON_DAY_CLICK.equals(eventName)){

			Calendars cal = (Calendars)comp;
			Date date =  (Date)event.getData();
			cal.setCurrentDate(date);

			p_CalendarMold = GroupwareToDoUtil.CALENDAR_ONEDAY_VIEW;
			//setCalendarMold(1);
			updateDateLabel();

		}else if (CalendarsEvent.ON_WEEK_CLICK.equals(eventName)){

			//I don't know this Event

		}else if(CalendarsEvent.ON_EVENT_UPDATE.equals(eventName)){

			if(event instanceof CalendarsEvent)	//Drag & Drop
			{
				CalendarsEvent calEvent = (CalendarsEvent) event;
				ToDoCalendarEvent todoEvent = (ToDoCalendarEvent) calEvent.getCalendarEvent();
				I_ToDo todo = todoEvent.getToDo();
				HashMap<Integer, ToDoCalendarEvent> events = null;

				if(todo.getJP_ToDo_Type().equals(MToDo.JP_TODO_TYPE_Schedule))
				{
					Timestamp startTime = new Timestamp(calEvent.getBeginDate().getTime());
					todo.setJP_ToDo_ScheduledStartTime(startTime);

					//Adjust
					Timestamp endTime = new Timestamp(calEvent.getEndDate().getTime());
					Timestamp schesuledEndTime = todo.getJP_ToDo_ScheduledEndTime();
					if(schesuledEndTime.toLocalDateTime().toLocalTime() == LocalTime.MIN)
					{
						endTime = Timestamp.valueOf(LocalDateTime.of(endTime.toLocalDateTime().toLocalDate(), LocalTime.MIN));
					}

					todo.setJP_ToDo_ScheduledEndTime(endTime);

					if(p_AD_User_ID == p_SelectedTab_AD_User_ID)
					{
						if(p_AD_User_ID == todo.getAD_User_ID())
						{
							events = map_AcquiredCalendarEvent_User.get(p_AD_User_ID);
						}else {
							events = map_AcquiredCalendarEvent_Team.get(todo.getAD_User_ID());
						}

					}else {

						events = map_AcquiredCalendarEvent_Team.get(p_SelectedTab_AD_User_ID);

					}

				}else if(todo.getJP_ToDo_Type().equals(MToDo.JP_TODO_TYPE_Task)) {

					todo.setJP_ToDo_ScheduledEndTime(new Timestamp(calEvent.getBeginDate().getTime()));
					if(p_AD_User_ID == p_SelectedTab_AD_User_ID)
					{
						if(p_AD_User_ID == todo.getAD_User_ID())
						{
							events = map_AcquiredCalendarEvent_User.get(p_AD_User_ID);
						}else {
							events = map_AcquiredCalendarEvent_Team.get(todo.getAD_User_ID());
						}
					}else {
						events = map_AcquiredCalendarEvent_Team.get(p_SelectedTab_AD_User_ID);
					}

				}

				ToDoCalendarEvent oldEvent = events.get(todo.get_ID());
				ToDoCalendarEvent newEvent = new ToDoCalendarEvent(todo);

				events.put(todo.get_ID(), newEvent);
				if(!todo.save())
				{
					//TODO エラー処理
				}


				updateCalendarEvent(oldEvent, newEvent);

				if(p_AD_User_ID == p_SelectedTab_AD_User_ID)
					refreshWest(todo.getJP_ToDo_Type());

			}

		}

	}



	/**
	 * Update Displayed Calender period.
	 */
	private void updateDateLabel()
	{
//		Date b = map_Calendars.get(p_SelectedTab_AD_User_ID).getBeginDate();
//		Date e = map_Calendars.get(p_SelectedTab_AD_User_ID).getEndDate();
//
//		LocalDateTime local = new Timestamp(e.getTime()).toLocalDateTime();
//		e = new Date(Timestamp.valueOf(local.minusDays(1)).getTime());
//
//		SimpleDateFormat sdfV = DisplayType.getDateFormat();
		//sdfV.setTimeZone(calendars.getDefaultTimeZone());

//		label_DisplayPeriod.setValue(sdfV.format(b) + " - " + sdfV.format(e));
	}



	private void deleteCalendarEvent(ToDoCalendarEvent deleteEvent)
	{
		if(deleteEvent != null)
		{
//			Calendars calendars = map_Calendars.get(p_SelectedTab_AD_User_ID);
//			SimpleCalendarModel	scm = (SimpleCalendarModel)calendars.getModel();
//			if(isAcquiredToDoCalendarEventRange(deleteEvent))
//			{
//				scm.remove(deleteEvent);
//			}
		}
	}

	private void createCalendarEvent(ToDoCalendarEvent newEvent)
	{
		if(isSkip(newEvent))
			return ;

//		Calendars calendars = map_Calendars.get(p_SelectedTab_AD_User_ID);
//		SimpleCalendarModel	scm = (SimpleCalendarModel)calendars.getModel();
//
//		if(isAcquiredToDoCalendarEventRange(newEvent))
//		{
//			setEventTextAndColor(newEvent);
//			scm.add(newEvent);
//		}

	}

	private void updateCalendarEvent(ToDoCalendarEvent oldEvent, ToDoCalendarEvent newEvent)
	{
//		Calendars calendars = map_Calendars.get(p_SelectedTab_AD_User_ID);
//		SimpleCalendarModel	scm = (SimpleCalendarModel)calendars.getModel();
//
//		if(oldEvent != null)
//			scm.remove(oldEvent);
//
//		if(isSkip(newEvent))
//			return ;
//
//		if(isAcquiredToDoCalendarEventRange(newEvent))
//		{
//			setEventTextAndColor(newEvent);
//			scm.add(newEvent);
//		}

	}


	private boolean isAcquiredToDoCalendarEventRange(ToDoCalendarEvent event)
	{
		if(event.getToDo().getJP_ToDo_Type().equals(MToDo.JP_TODO_TYPE_Schedule))
		{
			if(event.getToDo().getJP_ToDo_ScheduledStartTime().compareTo(ts_AcquiredToDoCalendarEventEnd) <= 0
					&& event.getToDo().getJP_ToDo_ScheduledEndTime().compareTo(ts_AcquiredToDoCalendarEventBegin) >= 0)
			{
				return true;
			}else {
				return false;
			}


		}else if(event.getToDo().getJP_ToDo_Type().equals(MToDo.JP_TODO_TYPE_Task)) {

			if(event.getToDo().getJP_ToDo_ScheduledEndTime().compareTo(ts_AcquiredToDoCalendarEventBegin) >= 0
					&& event.getToDo().getJP_ToDo_ScheduledEndTime().compareTo(ts_AcquiredToDoCalendarEventEnd) <= 0)
			{
				return true;
			}else {
				return false;
			}

		}

		return true;
	}


	Timestamp ts_AcquiredToDoCalendarEventBegin = null;
	Timestamp ts_AcquiredToDoCalendarEventEnd = null;




	/**
	 * Judge of to display ToDo.
	 *
	 * @param event
	 * @return
	 */
	private boolean isSkip(ToDoCalendarEvent event)
	{
		if(!p_IsDisplaySchedule)
		{
			if(event.getToDo().getJP_ToDo_Type().equals(MToDo.JP_TODO_TYPE_Schedule))
				return true;
		}

		if(!p_IsDisplayTask)
		{
			if(event.getToDo().getJP_ToDo_Type().equals(MToDo.JP_TODO_TYPE_Task))
				return true;
		}

		if(p_JP_ToDo_Category_ID > 0)
		{
			if(event.getToDo().getJP_ToDo_Category_ID() != p_JP_ToDo_Category_ID)
				return true;
		}

		if(!Util.isEmpty(p_JP_ToDo_Status))
		{
			if(MGroupwareUser.JP_TODO_STATUS_NotCompleted.equals(p_JP_ToDo_Status))
			{
				if(event.getToDo().getJP_ToDo_Status().equals(MGroupwareUser.JP_TODO_STATUS_NotYetStarted)
						|| event.getToDo().getJP_ToDo_Status().equals(MGroupwareUser.JP_TODO_STATUS_WorkInProgress))
				{
					;//Noting to do;

				}else {

					return true;
				}
			}else {

				if(!event.getToDo().getJP_ToDo_Status().equals(p_JP_ToDo_Status))
					return true;
			}

		}

			return true;

	}


	/**
	 * Get Main User's Calendar Event.
	 */
    private void queryToDoCalendarEvents_User()
    {

		StringBuilder whereClauseFinal = null;
		StringBuilder whereClauseSchedule = null;
		StringBuilder whereClauseTask = null;
		StringBuilder orderClause = null;
		ArrayList<Object> list_parameters  = new ArrayList<Object>();
		Object[] parameters = null;


		if(ts_AcquiredToDoCalendarEventBegin == null)
			ts_AcquiredToDoCalendarEventBegin = new Timestamp(0);

		if(ts_AcquiredToDoCalendarEventEnd == null)
			ts_AcquiredToDoCalendarEventEnd = new Timestamp(0);

		LocalDateTime toDayMin = LocalDateTime.of(ts_AcquiredToDoCalendarEventBegin.toLocalDateTime().toLocalDate(), LocalTime.MIN);
		LocalDateTime toDayMax = LocalDateTime.of(ts_AcquiredToDoCalendarEventEnd.toLocalDateTime().toLocalDate(), LocalTime.MAX);


		/**
		 *  SQL of Get Schedule
		 **/
		//AD_Client_ID
		whereClauseSchedule = new StringBuilder(" AD_Client_ID=? ");
		list_parameters.add(Env.getAD_Client_ID(ctx));

		//AD_User_ID
		whereClauseSchedule = whereClauseSchedule.append(" AND AD_User_ID = ? ");
		list_parameters.add(p_AD_User_ID);

		//JP_ToDo_ScheduledStartTime
		whereClauseSchedule = whereClauseSchedule.append(" AND JP_ToDo_ScheduledStartTime <= ? AND JP_ToDo_ScheduledEndTime >= ? AND IsActive='Y' ");//1 - 2
		list_parameters.add(Timestamp.valueOf(toDayMax));
		list_parameters.add(Timestamp.valueOf(toDayMin));

		//JP_TODO_TYPE_Schedule
		whereClauseSchedule = whereClauseSchedule.append(" AND JP_ToDo_Type = ? ");
		list_parameters.add(MToDo.JP_TODO_TYPE_Schedule);

		//Authorization Check
		if(p_login_User_ID == p_AD_User_ID)
		{
			//Noting to do;

		}else {

			whereClauseSchedule = whereClauseSchedule.append(" AND (IsOpenToDoJP='Y' OR CreatedBy = ?)");
			list_parameters.add(p_login_User_ID);
		}

		/**
		 *  SQL of Get Task
		 **/
		//AD_Client_ID
		whereClauseTask = new StringBuilder(" AD_Client_ID=? ");
		list_parameters.add(Env.getAD_Client_ID(ctx));

		//AD_User_ID
		whereClauseTask = whereClauseTask.append(" AND AD_User_ID = ? ");
		list_parameters.add(p_AD_User_ID);

		//JP_ToDo_ScheduledStartTime
		whereClauseTask =  whereClauseTask.append(" AND JP_ToDo_ScheduledEndTime <= ? AND JP_ToDo_ScheduledEndTime >= ? AND IsActive='Y' ");
		list_parameters.add(Timestamp.valueOf(toDayMax));
		list_parameters.add(Timestamp.valueOf(toDayMin));

		//JP_TODO_TYPE_Schedule
		whereClauseTask = whereClauseTask.append(" AND JP_ToDo_Type = ? ");
		list_parameters.add(MToDo.JP_TODO_TYPE_Task);

		//Authorization Check
		if(p_login_User_ID == p_AD_User_ID)
		{
			//Noting to do;

		}else {
			whereClauseTask = whereClauseTask.append(" AND (IsOpenToDoJP='Y' OR CreatedBy = ?)");
			list_parameters.add(p_login_User_ID);
		}


		/**
		 * Execution SQL
		 */

		whereClauseFinal = new StringBuilder("(").append( whereClauseSchedule.append(") OR (").append(whereClauseTask).append(")") );
		parameters = list_parameters.toArray(new Object[list_parameters.size()]);
		orderClause = new StringBuilder("AD_User_ID, JP_ToDo_ScheduledStartTime, JP_ToDo_ScheduledEndTime");

		if(MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo.equals(p_JP_ToDo_Calendar))//Search Personal ToDo
		{


			List<MToDo> list_ToDoes = new Query(Env.getCtx(), MToDo.Table_Name, whereClauseFinal.toString(), null)
												.setParameters(parameters)
												.setOrderBy(orderClause.toString())
												.list();


			if(list_ToDoes == null || list_ToDoes.size() == 0)
			{
				return ;
			}

			HashMap<Integer,ToDoCalendarEvent> eventMap = null;
			ToDoCalendarEvent event = null;

			for(MToDo todo :list_ToDoes)
			{
				event = new ToDoCalendarEvent(todo);
				eventMap = map_AcquiredCalendarEvent_User.get(p_AD_User_ID);
				if(eventMap == null)
				{
					eventMap = new HashMap<Integer, ToDoCalendarEvent>();
					eventMap.put(todo.get_ID(), event);
					map_AcquiredCalendarEvent_User.put(p_AD_User_ID, eventMap);
				}else {
					eventMap.put(todo.get_ID(), event);
				}

			}//for

		}else {//Search Team ToDo


			List<MToDoTeam> list_ToDoes = new Query(Env.getCtx(), MToDoTeam.Table_Name, whereClauseFinal.toString(), null)
												.setParameters(parameters)
												.setOrderBy(orderClause.toString())
												.list();


			if(list_ToDoes == null || list_ToDoes.size() == 0)
			{
				return ;
			}

			HashMap<Integer,ToDoCalendarEvent> eventMap = null;
			ToDoCalendarEvent event = null;

			for(MToDoTeam todo :list_ToDoes)
			{
				event = new ToDoCalendarEvent(todo);
				eventMap = map_AcquiredCalendarEvent_User.get(p_AD_User_ID);
				if(eventMap == null)
				{
					eventMap = new HashMap<Integer, ToDoCalendarEvent>();
					eventMap.put(todo.get_ID(), event);
					map_AcquiredCalendarEvent_User.put(p_AD_User_ID, eventMap);
				}else {
					eventMap.put(todo.get_ID(), event);
				}

			}//for
		}

		return ;
    }



	/**
	 * Get Team User's Calendar Event.
	 */
    private void queryToDoCalendarEvents_Team()
    {

    	if(p_JP_Team_ID == 0 || m_Team == null)
    	{
    		map_AcquiredCalendarEvent_Team.clear();
    		return ;
    	}

		StringBuilder whereClauseFinal = null;
		StringBuilder whereClauseSchedule = null;
		StringBuilder whereClauseTask = null;
		StringBuilder orderClause = null;
		ArrayList<Object> list_parameters  = new ArrayList<Object>();
		Object[] parameters = null;

		LocalDateTime toDayMin = LocalDateTime.of(ts_AcquiredToDoCalendarEventBegin.toLocalDateTime().toLocalDate(), LocalTime.MIN);
		LocalDateTime toDayMax = LocalDateTime.of(ts_AcquiredToDoCalendarEventEnd.toLocalDateTime().toLocalDate(), LocalTime.MAX);

		/**
		 *  SQL of Get Schedule
		 **/
		//AD_Client_ID
		whereClauseSchedule = new StringBuilder(" AD_Client_ID=? ");
		list_parameters.add(Env.getAD_Client_ID(ctx));

		//AD_User_ID
		whereClauseSchedule = whereClauseSchedule.append(" AND AD_User_ID IN (").append(createInUserClause(list_parameters)).append(")");

		//JP_ToDo_ScheduledStartTime
		whereClauseSchedule = whereClauseSchedule.append(" AND JP_ToDo_ScheduledStartTime <= ? AND JP_ToDo_ScheduledEndTime >= ? AND IsActive='Y' ");
		list_parameters.add(Timestamp.valueOf(toDayMax));
		list_parameters.add(Timestamp.valueOf(toDayMin));

		//JP_TODO_TYPE_Schedule
		whereClauseSchedule = whereClauseSchedule.append(" AND JP_ToDo_Type = ? ");
		list_parameters.add(MToDo.JP_TODO_TYPE_Schedule);

		//Authorization Check
		whereClauseSchedule = whereClauseSchedule.append(" AND (IsOpenToDoJP='Y' OR CreatedBy = ?)");
		list_parameters.add(p_login_User_ID);


		/**
		 *  SQL of Get Task
		 **/
		//AD_Client_ID
		whereClauseTask = new StringBuilder(" AD_Client_ID=? ");
		list_parameters.add(Env.getAD_Client_ID(ctx));

		//AD_User_ID
		whereClauseTask = whereClauseTask.append(" AND AD_User_ID IN (").append(createInUserClause(list_parameters)).append(")");

		//JP_ToDo_ScheduledStartTime
		whereClauseTask = whereClauseTask.append(" AND JP_ToDo_ScheduledEndTime <= ? AND JP_ToDo_ScheduledEndTime >= ? AND IsActive='Y' ");//1 - 2
		list_parameters.add(Timestamp.valueOf(toDayMax));
		list_parameters.add(Timestamp.valueOf(toDayMin));

		//JP_TODO_TYPE_Schedule
		whereClauseTask = whereClauseTask.append(" AND JP_ToDo_Type = ? ");
		list_parameters.add(MToDo.JP_TODO_TYPE_Task);

		//Authorization Check
		whereClauseTask = whereClauseTask.append(" AND (IsOpenToDoJP='Y' OR CreatedBy = ?)");
		list_parameters.add(p_login_User_ID);


		/**
		 * Execution SQL
		 */
		whereClauseFinal = new StringBuilder("(").append( whereClauseSchedule.append(") OR (").append(whereClauseTask).append(")") );
		parameters = list_parameters.toArray(new Object[list_parameters.size()]);
		orderClause = new StringBuilder("AD_User_ID, JP_ToDo_ScheduledStartTime, JP_ToDo_ScheduledEndTime");

		if(MGroupwareUser.JP_TODO_CALENDAR_PersonalToDo.equals(p_JP_ToDo_Calendar))//Search Personal ToDo
		{
			List<MToDo> list_ToDoes = new Query(Env.getCtx(), MToDo.Table_Name, whereClauseFinal.toString(), null)
											.setParameters(parameters)
											.setOrderBy(orderClause.toString())
											.list();

			if(list_ToDoes == null || list_ToDoes.size() == 0)
			{
				return ;
			}

			HashMap<Integer,ToDoCalendarEvent> eventMap = null;
			ToDoCalendarEvent event = null;

			for(MToDo todo :list_ToDoes)
			{
				event = new ToDoCalendarEvent(todo);
				eventMap = map_AcquiredCalendarEvent_Team.get(event.getToDo().getAD_User_ID());
				if(eventMap == null)
				{
					eventMap = new HashMap<Integer, ToDoCalendarEvent>();
					eventMap.put(todo.get_ID(), event);
					map_AcquiredCalendarEvent_Team.put(event.getToDo().getAD_User_ID(), eventMap);
				}else {
					eventMap.put(todo.get_ID(), event);
				}

			}//for

		}else { //Search Team ToDo

			List<MToDoTeam> list_ToDoes = new Query(Env.getCtx(), MToDoTeam.Table_Name, whereClauseFinal.toString(), null)
					.setParameters(parameters)
					.setOrderBy(orderClause.toString())
					.list();

			if(list_ToDoes == null || list_ToDoes.size() == 0)
			{
				return ;
			}

			HashMap<Integer,ToDoCalendarEvent> eventMap = null;
			ToDoCalendarEvent event = null;
			for(MToDoTeam todo :list_ToDoes)
			{
				event = new ToDoCalendarEvent(todo);
				eventMap = map_AcquiredCalendarEvent_Team.get(event.getToDo().getAD_User_ID());
				if(eventMap == null)
				{
					eventMap = new HashMap<Integer, ToDoCalendarEvent>();
					eventMap.put(todo.get_ID(), event);
					map_AcquiredCalendarEvent_Team.put(event.getToDo().getAD_User_ID(), eventMap);
				}else {
					eventMap.put(todo.get_ID(), event);
				}

			}//for

		}
		return ;
    }



    /**
     * Create SQL Sentence of AD_User_ID In ().
     *
     * @param list_parameters
     * @return
     */
    private String createInUserClause(ArrayList<Object> list_parameters)
    {

    	StringBuilder users = null;
    	String Q = ",?";

    	MTeamMember[] member = m_Team.getTeamMember();
    	for(int i = 0; i < member.length; i++)
    	{
    		if(p_AD_User_ID != member[i].getAD_User_ID())
    		{
    			if(users == null)
    			{
    				users = new StringBuilder("?");
    				list_parameters.add(member[i].getAD_User_ID());

    			}else {

		    		users = users.append(Q);
		    		list_parameters.add(member[i].getAD_User_ID());
    			}
    		}
    	}

    	return users.toString();

    }






	/**
	 * Get Personal ToDo List (Implement of I_ToDoPopupwindowCaller)
	 */
	@Override
	public List<I_ToDo> getToDoList()
	{
		return list_ToDoes;
	}


	/**
	 * Get Default AD_User_ID (Implement of I_CallerToDoPopupwindow)
	 */
	@Override
	public int getDefault_AD__User_ID()
	{
		return p_SelectedTab_AD_User_ID;
	}


	/**
	 * Get Default JP_ToDo_Category_ID (Implement of I_CallerToDoPopupwindow)
	 */
	@Override
	public int getDefault_JP_ToDo_Category_ID()
	{
		return p_JP_ToDo_Category_ID;
	}


	/**
	 * Get Default JP_ToDo_Type (Implement of I_CallerToDoPopupwindow)
	 */
	@Override
	public String getDefault_JP_ToDo_Type()
	{
		if(list_ToDoes == null)
		{
			if(p_IsDisplaySchedule && !p_IsDisplayTask)
			{
				return MToDo.JP_TODO_TYPE_Schedule;

			}else if (!p_IsDisplaySchedule && p_IsDisplayTask) {

				return MToDo.JP_TODO_TYPE_Task;

			}else if (!p_IsDisplaySchedule && !p_IsDisplayTask) {

				return MToDo.JP_TODO_TYPE_Memo;

			}

			return MToDo.JP_TODO_TYPE_Schedule;
		}else {

			return list_ToDoes.get(0).getJP_ToDo_Type();
		}

	}

	@Override
	public boolean update(I_ToDo todo)
	{
		ToDoCalendarEvent oldEvent = null;
		ToDoCalendarEvent newEvent = null;
		if(todo.getAD_User_ID() == p_AD_User_ID)
		{
			oldEvent = map_AcquiredCalendarEvent_User.get(todo.getAD_User_ID()).get(todo.get_ID());
			if(oldEvent != null)
				map_AcquiredCalendarEvent_User.get(todo.getAD_User_ID()).remove(todo.get_ID());

			newEvent = new ToDoCalendarEvent(todo);
			if(isAcquiredToDoCalendarEventRange(newEvent))
				map_AcquiredCalendarEvent_User.get(todo.getAD_User_ID()).put(todo.get_ID(), newEvent);

		}else {

			oldEvent = map_AcquiredCalendarEvent_Team.get(todo.getAD_User_ID()).get(todo.get_ID());
			if(oldEvent != null)
				map_AcquiredCalendarEvent_Team.get(todo.getAD_User_ID()).remove(todo.get_ID());

			newEvent = new ToDoCalendarEvent(todo);
			if(isAcquiredToDoCalendarEventRange(newEvent))
				map_AcquiredCalendarEvent_Team.get(todo.getAD_User_ID()).put(todo.get_ID(), newEvent);
		}

		updateCalendarEvent(oldEvent, newEvent);

		return true;
	}

	@Override
	public boolean create(I_ToDo todo)
	{
		ToDoCalendarEvent newEvent = null;
		if(todo.getAD_User_ID() == p_AD_User_ID)
		{
			newEvent = new ToDoCalendarEvent(todo);
			if(isAcquiredToDoCalendarEventRange(newEvent))
				map_AcquiredCalendarEvent_User.get(todo.getAD_User_ID()).put(todo.get_ID(), newEvent);

		}else {

			newEvent = new ToDoCalendarEvent(todo);
			if(isAcquiredToDoCalendarEventRange(newEvent))
			{
				HashMap<Integer,ToDoCalendarEvent> map_userEvent =  map_AcquiredCalendarEvent_Team.get(todo.getAD_User_ID());
				if(map_userEvent == null)
				{
					if(p_JP_Team_ID == 0 && m_Team == null)
					{
						;//Noting to do -> Don't display calendar

					}else {

						MTeamMember[] member = m_Team.getTeamMember();
						boolean isMember = false;
						for(int i = 0; i < member.length; i++)
						{
							if(member[i].getAD_User_ID() == todo.getAD_User_ID())
							{
								isMember = true;
								break;
							}
						}

						if(isMember)
						{
							map_userEvent = new HashMap<Integer,ToDoCalendarEvent>();
							map_userEvent.put(todo.get_ID(), newEvent);
							map_AcquiredCalendarEvent_Team.put(todo.getAD_User_ID(), map_userEvent);
						}else {
							return true;
						}
					}

				}else {
					map_userEvent.put(todo.get_ID(), newEvent);

				}
			}
		}

		createCalendarEvent(newEvent);

		return true;
	}

	@Override
	public boolean delete(I_ToDo deleteToDo)
	{

		ToDoCalendarEvent deleteEvent = null;
		if(deleteToDo.getAD_User_ID() == p_AD_User_ID)
		{
			deleteEvent = map_AcquiredCalendarEvent_User.get(deleteToDo.getAD_User_ID()).get(deleteToDo.get_ID());
			if(deleteEvent != null)
				map_AcquiredCalendarEvent_User.get(deleteToDo.getAD_User_ID()).remove(deleteToDo.get_ID());


		}else {

			deleteEvent = map_AcquiredCalendarEvent_Team.get(deleteToDo.getAD_User_ID()).get(deleteToDo.get_ID());
			if(deleteEvent != null)
				map_AcquiredCalendarEvent_Team.get(deleteToDo.getAD_User_ID()).remove(deleteToDo.get_ID());

		}

		deleteCalendarEvent(deleteEvent);


		return true;
	}


	@Override
	public boolean refresh(I_ToDo todo)
	{
		p_SelectedTab_AD_User_ID = p_AD_User_ID;
		p_OldSelectedTab_AD_User_ID = p_AD_User_ID;

		if(p_JP_Team_ID > 0)
		{
		  	mainBorderLayout_Center.getFirstChild().detach();
			mainBorderLayout_Center.appendChild(createCenterContents());

		}


		return true;
	}


	/**
	 * Refresh West components of Borderlayout
	 *
	 * @param JP_ToDo_Type
	 * @return boolean
	 */
	private boolean refreshWest(String JP_ToDo_Type)
	{
		personalToDoGadget_Schedule.setAD_User_ID(p_AD_User_ID);
		personalToDoGadget_Task.setAD_User_ID(p_AD_User_ID);
		personalToDoGadget_Memo.setAD_User_ID(p_AD_User_ID);

		teamToDoGadget_Schedule.setAD_User_ID(p_AD_User_ID);
		teamToDoGadget_Task.setAD_User_ID(p_AD_User_ID);
		teamToDoGadget_Memo.setAD_User_ID(p_AD_User_ID);

		return true;
	}

	private Timestamp p_CalendarsEventBeginDate = null;

	@Override
	public Timestamp getDefault_JP_ToDo_ScheduledStartTime()
	{
		Timestamp timestamp = null;
		if(p_CalendarsEventBeginDate == null)
		{
			timestamp = new Timestamp(0);
			LocalDateTime ldt = timestamp.toLocalDateTime();

			return Timestamp.valueOf(LocalDateTime.of(ldt.toLocalDate(), LocalTime.MIN));


		}else {

			timestamp = p_CalendarsEventBeginDate;

			return Timestamp.valueOf(LocalDateTime.of(timestamp.toLocalDateTime().toLocalDate(), timestamp.toLocalDateTime().toLocalTime()));
		}


	}

	private Timestamp p_CalendarsEventEndDate = null;

	@Override
	public Timestamp getDefault_JP_ToDo_ScheduledEndTime()
	{
		Timestamp timestamp = null;
		if(p_CalendarsEventEndDate == null)
		{
			timestamp = new Timestamp(0);
			LocalDateTime ldt = timestamp.toLocalDateTime();

			return Timestamp.valueOf(LocalDateTime.of(ldt.toLocalDate(), LocalTime.MIN));

		}else {

			timestamp =  p_CalendarsEventEndDate;

			if(GroupwareToDoUtil.CALENDAR_MONTH_VIEW.equals(p_CalendarMold))
			{
				timestamp = p_CalendarsEventBeginDate;

			}else if(GroupwareToDoUtil.CALENDAR_SEVENDAYS_VIEW.equals(p_CalendarMold) || GroupwareToDoUtil.CALENDAR_ONEDAY_VIEW.equals(p_CalendarMold)
																				|| GroupwareToDoUtil.CALENDAR_FIVEDAYS_VIEW.equals(p_CalendarMold) ) {

				LocalTime start = p_CalendarsEventBeginDate.toLocalDateTime().toLocalTime();
				LocalTime end = p_CalendarsEventEndDate.toLocalDateTime().toLocalTime();

				if(start.compareTo(LocalTime.MIN) == 0 && end.compareTo(LocalTime.MIN) == 0)
				{
					timestamp = p_CalendarsEventBeginDate;
				}

			}

			return Timestamp.valueOf(LocalDateTime.of(timestamp.toLocalDateTime().toLocalDate(), timestamp.toLocalDateTime().toLocalTime()));
		}


	}


	@Override
	public int getWindowNo()
	{
		return form.getWindowNo();
	}



	@Override
	public String getJP_ToDo_Calendar()
	{
		return p_JP_ToDo_Calendar;
	}


}
