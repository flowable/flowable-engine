/// <reference types="cheerio" />
/**
 * truncate-html full options object
 */
export interface IFullOptions {
    /**
     * remove all tags, default false
     */
    stripTags: boolean;
    /**
     * ellipsis sign, default '...'
     */
    ellipsis: string;
    /**
     * decode html entities(e.g. convert `&amp;` to `&`) before counting length, default false
     */
    decodeEntities: boolean;
    /**
     * elements' selector you want ignore
     */
    excludes: string | string[];
    /**
     * how many letters(words if `byWords` is true) you want reserve
     */
    length: number;
    /**
     * if true, length means how many words to reserve
     */
    byWords: boolean;
    /**
     * how to deal with when truncate in the middle of a word
     *  1. by default, just cut at that position.
     *  2. set it to true, with max exceed 10 letters can exceed to reserver the last word
     *  3. set it to a positive number decide how many letters can exceed to reserve the last word
     *  4. set it to negetive number to remove the last word if cut in the middle.
     */
    reserveLastWord: boolean | number;
    /**
     * keep whitespaces, by default continuous paces will
     *  be replaced with one space, set it true to keep them
     */
    keepWhitespaces: boolean;
}
/**
 * options interface for function
 */
export declare type IOptions = Partial<IFullOptions>;
/**
 * truncate html interface
 */
interface ITruncateHtml {
    (html: string | CheerioStatic, length?: number, options?: IOptions): string;
    (html: string | CheerioStatic, options?: IOptions): string;
    setup: (option: IOptions) => void;
}
/**
 * truncate html
 * @method truncate(html, [length], [options])
 * @param  {String}         html    html string to truncate
 * @param  {Object|number}  length how many letters(words if `byWords` is true) you want reserve
 * @param  {Object|null}    options
 * @return {String}
 */
declare const truncate: ITruncateHtml;
export default truncate;
