import { createUniqueId, onCleanup } from 'solid-js';

const MARGIN = 8;

export function InfoButton(props: { text: string; label: string }) {
  const id = createUniqueId();
  let button!: HTMLButtonElement;
  let popover!: HTMLDivElement;
  const hide = () => popover.hidePopover();
  const place = (event: ToggleEvent) => {
    if (event.newState !== 'open') {
      window.removeEventListener('scroll', hide, true);
      return;
    }
    window.addEventListener('scroll', hide, true);
    const anchor = button.getBoundingClientRect();
    const width = popover.offsetWidth;
    const height = popover.offsetHeight;
    const left = Math.max(MARGIN, Math.min(anchor.left, window.innerWidth - width - MARGIN));
    const below = anchor.bottom + 4;
    const top = height > 0 && below + height > window.innerHeight - MARGIN ? Math.max(MARGIN, anchor.top - height - 4) : below;
    popover.style.left = `${left}px`;
    popover.style.top = `${top}px`;
  };
  onCleanup(() => window.removeEventListener('scroll', hide, true));
  return (
    <>
      <button ref={button} type="button" class="info-button" popovertarget={id} aria-label={`About ${props.label}`} title={`About ${props.label}`}>
        ?
      </button>
      <div ref={popover} id={id} popover="auto" class="info-popover" onBeforeToggle={place} onToggle={place}>
        {props.text}
      </div>
    </>
  );
}
