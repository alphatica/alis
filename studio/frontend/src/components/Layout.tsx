import { useLocation, useNavigate } from '@solidjs/router';
import { createEffect, For, type JSX, Show } from 'solid-js';
import { FEATURE_REQUEST_URL, setHelpDialog, WEBSITE_URL } from '../pages/Help';
import { importPortfolioFile, setPortfolioDialog } from '../pages/PortfolioForms';
import { dataStatus } from '../state';

/** Entry of a screen's drop-down menu: switches to the screen, then runs the action; or opens an external page in a new tab. */
type MenuItem = { label: string; run: () => void } | { label: string; href: string };
type NavItem = { path: string; label: string; menu?: MenuItem[] };
/** Several screens under one name in the top bar; the group itself has no screen. */
type NavGroup = { label: string; screens: NavItem[] };

export const NAVIGATION: (NavItem | NavGroup)[] = [
  {
    path: '/portfolio',
    label: 'Portfolio',
    menu: [
      { label: 'Add trade…', run: () => setPortfolioDialog('trade') },
      { label: 'Deposit / Withdraw cash…', run: () => setPortfolioDialog('cash') },
      { label: 'Import from file…', run: importPortfolioFile },
    ],
  },
  { path: '/signals', label: 'Signals' },
  {
    label: 'Research',
    screens: [
      { path: '/backtest', label: 'Backtest' },
      { path: '/optimization', label: 'Optimization' },
      { path: '/better-exits', label: 'Better exits' },
    ],
  },
  { path: '/data', label: 'Data' },
  {
    path: '/help',
    label: 'Help',
    menu: [
      { label: 'Website', href: WEBSITE_URL },
      { label: 'License…', run: () => setHelpDialog('license') },
      { label: 'Feature request', href: FEATURE_REQUEST_URL },
      { label: 'About…', run: () => setHelpDialog('about') },
    ],
  },
];

/** All screens in the navigation, also those inside groups, in the order of the top bar. */
export const SCREENS: NavItem[] = NAVIGATION.flatMap((item) => ('screens' in item ? item.screens : [item]));

export function labelOf(path: string): string | undefined {
  return SCREENS.find((item) => item.path === path)?.label;
}

/** Top bar without navigation, for the license and closed screens. */
export function Shell(props: { title: string; children: JSX.Element }) {
  createEffect(() => (document.title = `${props.title} · Alis Studio`));
  return (
    <>
      <header class="topbar">
        <span class="brand">Alis</span>
      </header>
      <main class="page">{props.children}</main>
    </>
  );
}

export function Layout(props: { onQuit: () => void; children?: JSX.Element }) {
  const location = useLocation();
  createEffect(() => (document.title = `${labelOf(location.pathname) ?? 'Not found'} · Alis Studio`));
  return (
    <>
      <header class="topbar">
        <nav class="nav">
          <For each={NAVIGATION}>
            {(item) =>
              'screens' in item ? (
                <NavGroupMenu group={item} />
              ) : (
                <Show when={item.menu} fallback={<NavLink path={item.path}>{item.label}</NavLink>}>
                  {(menu) => <NavMenu item={item} menu={menu()} />}
                </Show>
              )
            }
          </For>
        </nav>
        <div class="topbar-right">
          <Show when={dataStatus()}>
            <span class="data-status">{dataStatus()}</span>
          </Show>
          <button class="btn" onClick={() => props.onQuit()}>
            Quit
          </button>
        </div>
      </header>
      <main class="page">{props.children}</main>
    </>
  );
}

/**
 * Screen link with a drop-down menu, shown while the pointer is over it or the keyboard focus is in it
 * (the arrow button opens it with the keyboard).
 */
function NavMenu(props: { item: NavItem; menu: MenuItem[] }) {
  const navigate = useNavigate();
  const location = useLocation();
  let wrapper!: HTMLDivElement;
  const close = () => {
    (document.activeElement as HTMLElement | null)?.blur();
    wrapper.classList.add('nav-menu-closed');
  };
  const choose = (run: () => void) => {
    close();
    if (location.pathname !== props.item.path) {
      navigate(props.item.path);
    }
    run();
  };
  return (
    <div ref={wrapper} class="nav-menu" onMouseLeave={() => wrapper.classList.remove('nav-menu-closed')}>
      <NavLink path={props.item.path}>{props.item.label}</NavLink>
      <button class="nav-menu-toggle" type="button" aria-haspopup="menu" aria-label={`${props.item.label} menu`}>
        ▾
      </button>
      <div class="nav-menu-items" role="menu">
        <For each={props.menu}>
          {(entry) =>
            'href' in entry ? (
              <a role="menuitem" href={entry.href} target="_blank" rel="noopener noreferrer" onClick={close}>
                {entry.label} ↗
              </a>
            ) : (
              <button type="button" role="menuitem" onClick={() => choose(entry.run)}>
                {entry.label}
              </button>
            )
          }
        </For>
      </div>
    </div>
  );
}

/** Group of screens: a drop-down list of their links, shown like the screen menus. Marked as current when one of its screens is. */
function NavGroupMenu(props: { group: NavGroup }) {
  const location = useLocation();
  let wrapper!: HTMLDivElement;
  const close = () => {
    (document.activeElement as HTMLElement | null)?.blur();
    wrapper.classList.add('nav-menu-closed');
  };
  const current = () => props.group.screens.some((screen) => screen.path === location.pathname);
  return (
    <div ref={wrapper} class="nav-menu" onMouseLeave={() => wrapper.classList.remove('nav-menu-closed')}>
      <button class="nav-group-toggle" type="button" aria-haspopup="menu" data-current={current() ? '' : undefined}>
        {props.group.label} <span class="nav-group-arrow">▾</span>
      </button>
      <div class="nav-menu-items" role="menu">
        <For each={props.group.screens}>
          {(screen) => (
            <NavLink path={screen.path} role="menuitem" onNavigate={close}>
              {screen.label}
            </NavLink>
          )}
        </For>
      </div>
    </div>
  );
}

/**
 * Navigation starts on mousedown instead of click: a click takes about 100 ms between press and release.
 * Only plain left clicks; modifier and middle clicks keep opening new tabs.
 */
function NavLink(props: { path: string; class?: string; role?: 'menuitem'; onNavigate?: () => void; children: JSX.Element }) {
  const location = useLocation();
  const navigate = useNavigate();
  const isPlainClick = (event: MouseEvent) =>
    event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey;
  const go = (event: MouseEvent) => {
    if (isPlainClick(event)) {
      event.preventDefault();
      if (location.pathname !== props.path) {
        navigate(props.path);
      }
      props.onNavigate?.();
    }
  };
  return (
    <a
      href={props.path}
      class={props.class}
      role={props.role}
      aria-current={location.pathname === props.path ? 'page' : undefined}
      onMouseDown={go}
      onClick={go}
    >
      {props.children}
    </a>
  );
}
