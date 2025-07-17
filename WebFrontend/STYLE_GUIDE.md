# 前端样式指南

本文档提供了项目中使用的样式系统的详细说明，以及如何在组件中应用这些样式。

## 目录

1. [样式架构](#样式架构)
2. [颜色系统](#颜色系统)
3. [排版](#排版)
4. [间距与尺寸](#间距与尺寸)
5. [阴影与边框](#阴影与边框)
6. [布局组件](#布局组件)
7. [工具类](#工具类)
8. [Element Plus 主题](#element-plus-主题)
9. [最佳实践](#最佳实践)

## 样式架构

样式系统分为三个主要文件：

- **base.css**: 定义基础变量和重置样式
- **main.css**: 提供通用组件样式和工具类
- **element-plus-theme.css**: 自定义 Element Plus 组件样式

所有样式都使用 CSS 变量进行定义，便于主题定制和维护。

## 颜色系统

我们使用以下颜色变量：

```css
--primary-color: #5E6AD2;      /* 主要颜色 */
--primary-hover: #4F5ABF;      /* 主要颜色悬停状态 */
--primary-light: #E6E8FA;      /* 主要颜色淡化版 */
--primary-dark: #3F4BBF;       /* 主要颜色加深版 */

--success-color: #67C23A;      /* 成功颜色 */
--warning-color: #E6A23C;      /* 警告颜色 */
--danger-color: #F56C6C;       /* 危险颜色 */
--info-color: #909399;         /* 信息颜色 */

--text-primary: #2C3E50;       /* 主要文本颜色 */
--text-secondary: #606F7B;     /* 次要文本颜色 */
--text-tertiary: #909399;      /* 第三级文本颜色 */
--text-light: #FFFFFF;         /* 浅色文本颜色 */

--bg-primary: #FFFFFF;         /* 主要背景颜色 */
--bg-secondary: #F5F7FA;       /* 次要背景颜色 */
--bg-tertiary: #EBEEF5;        /* 第三级背景颜色 */
--bg-sidebar: #1E293B;         /* 侧边栏背景颜色 */
--bg-sidebar-active: #293548;  /* 侧边栏激活状态背景颜色 */

--border-color: #E2E8F0;       /* 边框颜色 */
--border-color-light: #EBEEF5; /* 浅色边框颜色 */
--border-color-dark: #DCDFE6;  /* 深色边框颜色 */
```

使用示例：

```html
<div style="color: var(--text-primary); background-color: var(--bg-secondary);">
  示例文本
</div>
```

也可以使用预定义的文本颜色类：

```html
<span class="text-primary">主要文本</span>
<span class="text-secondary">次要文本</span>
<span class="text-tertiary">第三级文本</span>
<span class="text-light">浅色文本</span>
```

## 排版

字体大小变量：

```css
--text-xs: 12px;   /* 超小字体 */
--text-sm: 14px;   /* 小字体 */
--text-md: 16px;   /* 中等字体 */
--text-lg: 18px;   /* 大字体 */
--text-xl: 20px;   /* 超大字体 */
--text-2xl: 24px;  /* 特大字体 */
```

## 间距与尺寸

间距变量用于保持一致的组件间距：

```css
--spacing-xs: 4px;   /* 超小间距 */
--spacing-sm: 8px;   /* 小间距 */
--spacing-md: 16px;  /* 中等间距 */
--spacing-lg: 24px;  /* 大间距 */
--spacing-xl: 32px;  /* 超大间距 */
```

边框圆角：

```css
--radius-sm: 4px;      /* 小圆角 */
--radius-md: 8px;      /* 中等圆角 */
--radius-lg: 12px;     /* 大圆角 */
--radius-full: 9999px; /* 完全圆角（圆形） */
```

## 阴影与边框

阴影变量：

```css
--shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);                                              /* 小阴影 */
--shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);       /* 中等阴影 */
--shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);     /* 大阴影 */
--shadow-card: 0 2px 12px 0 rgba(0, 0, 0, 0.05);                                          /* 卡片阴影 */
--shadow-card-hover: 0 10px 20px rgba(0, 0, 0, 0.1);                                      /* 卡片悬停阴影 */
```

## 布局组件

### 容器

使用 `.container` 类作为页面主容器：

```html
<div class="container">
  <!-- 页面内容 -->
</div>
```

### 页面标题

使用 `.page-header` 类作为页面标题区域：

```html
<div class="page-header">
  <h1>页面标题</h1>
  <p>页面描述文本</p>
</div>
```

### 卡片

使用 `.card` 类创建卡片：

```html
<div class="card">
  <div class="card-header">卡片标题</div>
  <div class="card-body">卡片内容</div>
  <div class="card-footer">卡片底部</div>
</div>
```

### 搜索卡片与表格卡片

```html
<!-- 搜索卡片 -->
<el-card class="search-card">
  <!-- 搜索表单 -->
</el-card>

<!-- 表格卡片 -->
<el-card class="table-card">
  <!-- 表格内容 -->
</el-card>
```

### 网格布局

使用 `.grid` 类结合 `.grid-2`, `.grid-3`, `.grid-4` 或 `.grid-auto` 创建网格布局：

```html
<!-- 两列网格 -->
<div class="grid grid-2">
  <div>列 1</div>
  <div>列 2</div>
</div>

<!-- 自动网格 (根据内容自动调整) -->
<div class="grid grid-auto">
  <el-card class="card-item">项目 1</el-card>
  <el-card class="card-item">项目 2</el-card>
  <el-card class="card-item">项目 3</el-card>
</div>
```

## 工具类

### 间距工具类

```html
<div class="mt-sm">上方小间距</div>
<div class="mb-md">下方中等间距</div>
<div class="mr-lg">右侧大间距</div>
<div class="ml-xl">左侧超大间距</div>
```

### 弹性盒子工具类

```html
<div class="flex">弹性盒子</div>
<div class="flex flex-col">垂直弹性盒子</div>
<div class="flex items-center">垂直居中</div>
<div class="flex justify-between">两端对齐</div>
<div class="flex gap-md">中等间隔</div>
<div class="flex-1">占用剩余空间</div>
```

### 圆角工具类

```html
<div class="radius-sm">小圆角</div>
<div class="radius-md">中等圆角</div>
<div class="radius-lg">大圆角</div>
<div class="radius-full">完全圆角</div>
```

### 阴影工具类

```html
<div class="shadow-sm">小阴影</div>
<div class="shadow-md">中等阴影</div>
<div class="shadow-lg">大阴影</div>
<div class="shadow-card">卡片阴影</div>
```

## Element Plus 主题

我们已经自定义了 Element Plus 组件的样式，使其与我们的样式系统保持一致。无需添加额外类，Element Plus 组件将自动使用我们的主题样式。

## 最佳实践

1. **优先使用工具类**：尽可能使用预定义的工具类，避免编写自定义样式。
2. **组件样式**：对于组件特有的样式，使用 `scoped` 样式，并尽量使用变量而非硬编码值。
3. **保持一致**：使用预定义的间距、颜色和字体大小，确保整个应用的视觉一致性。
4. **响应式设计**：使用提供的 grid 类和响应式工具类，确保在不同屏幕尺寸下有良好表现。
5. **避免覆盖 Element Plus 样式**：尽量不要直接覆盖 Element Plus 组件的默认样式，如需自定义，请使用我们的主题变量。

---

如有任何关于样式系统的问题，请联系前端团队。 