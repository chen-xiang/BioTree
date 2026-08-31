/**
 * 中文界面文案。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 补充浏览与管理分类文案
 * Updated: 2026-08-31 补充配图管理文案
 * Updated: 2026-08-31 补充加载更多与节点移动文案
 */
export default {
  common: {
    brand: 'BioTree',
    loading: '加载中…',
    save: '保存',
    cancel: '取消',
  },
  nav: {
    home: '首页',
    browse: '浏览',
    admin: '管理',
    login: '登录',
  },
  home: {
    headline: '探索生命的层级',
    subtitle: '按界门纲目科属种浏览、检索与管理分类单元。',
    ctaBrowse: '开始浏览',
    ctaAdmin: '进入管理',
  },
  browse: {
    title: '分类浏览',
    subtitle: '懒加载展开层级，支持学名与俗名搜索。',
    tree: '分类树',
    search: '搜索',
    searchPlaceholder: '输入学名或俗名',
    searchResults: '搜索结果',
    selectHint: '从左侧选择一个分类单元查看详情。',
    childrenCount: '{n} 个子节点',
    loadMore: '加载更多',
  },
  theme: {
    light: '浅色',
    dark: '深色',
    toggle: '切换主题',
  },
  locale: {
    toggle: '切换语言',
  },
  admin: {
    title: '管理后台',
    placeholder: '从侧栏进入分类管理，维护节点与多语言介绍。',
    taxaNav: '分类管理',
    taxaTitle: '分类管理',
    taxaSubtitle: '按父节点维护子分类；有子节点时不可删除。',
    root: '根（界）',
    currentParent: '当前父节点',
    children: '子分类',
    enter: '进入',
    delete: '删除',
    empty: '暂无子节点',
    create: '新建分类',
    edit: '编辑分类',
    rank: '等级',
    scientificName: '学名',
    commonName: '俗名',
    summary: '摘要',
    description: '详细介绍',
    created: '已创建',
    updated: '已更新',
    deleted: '已删除',
    confirmDelete: '确认删除该分类？',
    media: '配图',
    mediaCaption: '图注',
    mediaHint: '支持 jpeg/png/webp/gif，最大 5MB。',
    mediaNoCaption: '无图注',
    mediaUploaded: '配图已上传',
    mediaDeleted: '配图已删除',
    confirmDeleteMedia: '确认删除该配图？',
    move: '移动到当前父节点',
    moved: '已移动',
    moveHint: '将节点移动到当前正在浏览的父节点下（等级须合法）。',
  },
  login: {
    title: '管理员登录',
    username: '用户名',
    password: '密码',
    submit: '登录',
  },
}
