declare module 'page-flip' {
  export class PageFlip {
    constructor(element: HTMLElement, settings: Record<string, any>)
    loadFromHTML(items: NodeListOf<HTMLElement> | HTMLElement[]): void
    loadFromImages(images: string[]): void
    updateFromHtml(items: NodeListOf<HTMLElement> | HTMLElement[]): void
    on(event: string, callback: (e: { data: any; object: PageFlip }) => void): PageFlip
    destroy(): void
    getPageCount(): number
    getCurrentPageIndex(): number
    flipNext(corner?: 'top' | 'bottom'): void
    flipPrev(corner?: 'top' | 'bottom'): void
    flip(pageNum: number, corner?: 'top' | 'bottom'): void
    turnToPage(pageNum: number): void
  }
}
