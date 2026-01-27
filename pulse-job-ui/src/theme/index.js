/**
 * Naive UI 深度定制主题配置
 * 文档: https://www.naiveui.com/zh-CN/os-theme/docs/customize-theme
 */

// 主色调
const primaryColor = '#5E81F4'
const primaryColorHover = '#7B9CF5'
const primaryColorPressed = '#4A6CD4'
const primaryColorSuppl = '#7B9CF5'

// 通用主题覆盖
export const themeOverrides = {
  common: {
    // 主色
    primaryColor,
    primaryColorHover,
    primaryColorPressed,
    primaryColorSuppl,
    
    // 信息色
    infoColor: '#5E81F4',
    infoColorHover: '#7B9CF5',
    infoColorPressed: '#4A6CD4',
    
    // 成功色
    successColor: '#10b981',
    successColorHover: '#34d399',
    successColorPressed: '#059669',
    
    // 警告色
    warningColor: '#f59e0b',
    warningColorHover: '#fbbf24',
    warningColorPressed: '#d97706',
    
    // 错误色
    errorColor: '#ef4444',
    errorColorHover: '#f87171',
    errorColorPressed: '#dc2626',
    
    // 文字颜色
    textColorBase: '#11142d',
    textColor1: '#11142d',
    textColor2: '#6f767e',
    textColor3: '#808191',
    textColorDisabled: '#b2b3bd',
    
    // 背景色
    bodyColor: '#f4f5f7',
    cardColor: '#ffffff',
    modalColor: '#ffffff',
    popoverColor: '#ffffff',
    
    // 边框
    borderColor: '#eff0f6',
    borderRadius: '8px',
    borderRadiusSmall: '6px',
    
    // 字体
    fontFamily: "'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    fontSize: '14px',
    fontSizeMini: '12px',
    fontSizeTiny: '12px',
    fontSizeSmall: '13px',
    fontSizeMedium: '14px',
    fontSizeLarge: '15px',
    fontSizeHuge: '16px',
    
    // 高度
    heightTiny: '28px',
    heightSmall: '32px',
    heightMedium: '36px',
    heightLarge: '40px',
    heightHuge: '44px'
  },
  
  // 按钮定制
  Button: {
    textColor: '#6f767e',
    borderRadiusTiny: '6px',
    borderRadiusSmall: '6px',
    borderRadiusMedium: '6px',
    borderRadiusLarge: '8px',
    // 主要按钮
    colorPrimary: primaryColor,
    colorHoverPrimary: primaryColor,
    colorPressedPrimary: primaryColor,
    colorFocusPrimary: primaryColor,
    textColorPrimary: '#ffffff',
    // 默认按钮
    color: '#ffffff',
    colorHover: '#ffffff',
    colorPressed: '#ffffff',
    border: '1px solid #eff0f6',
    borderHover: '1px solid #eff0f6',
    borderPressed: '1px solid #eff0f6',
    borderFocus: '1px solid #eff0f6',
    textColorHover: '#6f767e',
    textColorPressed: '#6f767e',
    textColorFocus: '#6f767e',
    // 去掉波纹效果
    waveOpacity: 0
  },
  
  // 输入框定制
  Input: {
    borderRadius: '8px',
    border: '1px solid #eff0f6',
    borderHover: '1px solid #eff0f6',
    borderFocus: '1px solid #eff0f6',
    boxShadowFocus: 'none',
    color: '#ffffff',
    colorFocus: '#ffffff',
    textColor: '#11142d',
    placeholderColor: '#b2b3bd',
    caretColor: primaryColor,
    heightMedium: '40px',
    paddingMedium: '0 14px'
  },
  
  // 选择器定制
  Select: {
    peers: {
      InternalSelection: {
        borderRadius: '8px',
        border: '1px solid #eff0f6',
        borderHover: '1px solid #eff0f6',
        borderFocus: '1px solid #eff0f6',
        boxShadowFocus: 'none',
        heightMedium: '40px'
      }
    }
  },
  
  // 复选框定制
  Checkbox: {
    borderRadius: '6px',
    border: '2px solid #eff0f6',
    borderChecked: `2px solid ${primaryColor}`,
    borderFocus: `2px solid ${primaryColor}`,
    colorChecked: primaryColor,
    checkMarkColor: '#ffffff',
    boxShadowFocus: 'none',
    sizeMedium: '20px'
  },
  
  // 开关定制
  Switch: {
    railColorActive: primaryColor,
    boxShadowFocus: 'none'
  },
  
  // 卡片定制
  Card: {
    borderRadius: '12px',
    borderColor: '#eff0f6',
    color: '#ffffff',
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    paddingMedium: '20px',
    paddingLarge: '24px'
  },
  
  // 对话框定制
  Dialog: {
    borderRadius: '16px',
    padding: '24px',
    titleFontSize: '18px',
    titleFontWeight: '600'
  },
  
  // 消息定制
  Message: {
    borderRadius: '8px',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
  },
  
  // 表格定制
  DataTable: {
    borderRadius: '12px',
    borderColor: '#eff0f6',
    // 表头
    thColor: 'transparent',
    thColorHover: 'transparent',
    thTextColor: '#b2b3bd',
    thFontWeight: '500',
    thPaddingSmall: '12px 16px',
    thPaddingMedium: '12px 16px',
    thPaddingLarge: '14px 18px',
    // 单元格
    tdColor: 'transparent',
    tdColorHover: '#f9fafb',
    tdTextColor: '#808191',
    tdPaddingSmall: '12px 16px',
    tdPaddingMedium: '12px 16px',
    tdPaddingLarge: '14px 18px',
    // 字体
    fontSizeSmall: '13px',
    fontSizeMedium: '13px',
    fontSizeLarge: '14px'
  },
  
  // 标签页定制
  Tabs: {
    tabTextColorLine: '#6f767e',
    tabTextColorActiveLine: primaryColor,
    tabTextColorHoverLine: primaryColor,
    barColor: primaryColor,
    tabFontWeightActive: '600'
  },
  
  // 菜单定制
  Menu: {
    borderRadius: '8px',
    itemTextColor: '#6f767e',
    itemTextColorHover: primaryColor,
    itemTextColorActive: primaryColor,
    itemTextColorActiveHover: primaryColor,
    itemColorActive: 'rgba(94, 129, 244, 0.1)',
    itemColorActiveHover: 'rgba(94, 129, 244, 0.15)',
    itemIconColor: '#6f767e',
    itemIconColorHover: primaryColor,
    itemIconColorActive: primaryColor
  },
  
  // 标签定制
  Tag: {
    borderRadius: '16px',
    padding: '0 12px',
    heightMedium: '28px'
  },
  
  // 徽标定制
  Badge: {
    color: '#ff6b6b'
  },
  
  // 分页定制
  Pagination: {
    itemBorderRadius: '8px',
    itemColorActive: primaryColor,
    itemTextColorActive: '#ffffff',
    itemTextColorHover: primaryColor,
    itemBorderActive: `1px solid ${primaryColor}`
  },
  
  // 进度条定制
  Progress: {
    fillColor: primaryColor,
    railColor: '#eff0f6'
  },
  
  // 滑块定制
  Slider: {
    fillColor: primaryColor,
    fillColorHover: primaryColorHover,
    handleColor: '#ffffff',
    dotBorderActive: `2px solid ${primaryColor}`
  },
  
  // 日期选择器定制
  DatePicker: {
    itemColorActive: primaryColor,
    itemTextColorActive: '#ffffff',
    itemBorderRadius: '8px'
  },
  
  // 抽屉定制
  Drawer: {
    borderRadius: '16px 0 0 16px'
  },
  
  // 下拉菜单定制
  Dropdown: {
    borderRadius: '12px',
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.12)',
    optionColorHover: 'rgba(94, 129, 244, 0.1)',
    optionTextColorHover: primaryColor
  },
  
  // 弹出层定制
  Popover: {
    borderRadius: '12px',
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.12)'
  },
  
  // 工具提示定制
  Tooltip: {
    borderRadius: '8px'
  },
  
  // 表单定制
  Form: {
    labelFontWeight: '500',
    labelTextColor: '#6f767e',
    feedbackTextColorError: '#ef4444',
    feedbackTextColorWarning: '#f59e0b'
  }
}

// 导出配置好的主题
export default themeOverrides
