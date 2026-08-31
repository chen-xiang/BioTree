/**
 * English UI copy.
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 Add browse and admin taxon copy
 * Updated: 2026-08-31 Add media management copy
 * Updated: 2026-08-31 Add load-more and move copy
 */
export default {
  common: {
    brand: 'BioTree',
    loading: 'Loading…',
    save: 'Save',
    cancel: 'Cancel',
  },
  nav: {
    home: 'Home',
    browse: 'Browse',
    admin: 'Admin',
    login: 'Sign in',
  },
  home: {
    headline: 'Explore the tree of life',
    subtitle: 'Browse, search, and manage taxa across kingdom to species.',
    ctaBrowse: 'Start browsing',
    ctaAdmin: 'Open admin',
  },
  browse: {
    title: 'Taxon browser',
    subtitle: 'Lazy-load the hierarchy and search by scientific or common name.',
    tree: 'Tree',
    search: 'Search',
    searchPlaceholder: 'Scientific or common name',
    searchResults: 'Search results',
    selectHint: 'Select a taxon on the left to view details.',
    childrenCount: '{n} children',
    loadMore: 'Load more',
    synonyms: 'Synonyms',
  },
  theme: {
    light: 'Light',
    dark: 'Dark',
    toggle: 'Toggle theme',
  },
  locale: {
    toggle: 'Switch language',
  },
  admin: {
    title: 'Admin console',
    placeholder: 'Use the sidebar to manage taxa and multilingual descriptions.',
    taxaNav: 'Taxa',
    taxaTitle: 'Manage taxa',
    taxaSubtitle: 'Maintain children under a parent. Deletion is blocked when children exist.',
    root: 'Root (kingdom)',
    currentParent: 'Current parent',
    children: 'Children',
    enter: 'Open',
    delete: 'Delete',
    empty: 'No children',
    create: 'Create taxon',
    edit: 'Edit taxon',
    rank: 'Rank',
    scientificName: 'Scientific name',
    commonName: 'Common name',
    summary: 'Summary',
    description: 'Description',
    created: 'Created',
    updated: 'Updated',
    deleted: 'Deleted',
    confirmDelete: 'Delete this taxon?',
    media: 'Media',
    mediaCaption: 'Caption',
    mediaHint: 'jpeg/png/webp/gif, max 5MB.',
    mediaNoCaption: 'No caption',
    mediaUploaded: 'Media uploaded',
    mediaDeleted: 'Media deleted',
    confirmDeleteMedia: 'Delete this image?',
    move: 'Move under current parent',
    moved: 'Moved',
    moveHint: 'Move this taxon under the parent you are currently browsing (rank must be valid).',
  },
  login: {
    title: 'Admin sign in',
    username: 'Username',
    password: 'Password',
    submit: 'Sign in',
  },
}
